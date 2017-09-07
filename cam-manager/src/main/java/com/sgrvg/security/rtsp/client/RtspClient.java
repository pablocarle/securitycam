package com.sgrvg.security.rtsp.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.temporal.ChronoUnit;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sgrvg.security.ServerConfigHolder;
import com.sgrvg.security.SimpleLogger;
import com.sgrvg.security.rtp.server.RTPServerHandle;
import com.sgrvg.security.rtsp.RtspServerDefinition;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.rtsp.RtspDecoder;
import io.netty.handler.codec.rtsp.RtspEncoder;
import io.netty.handler.timeout.WriteTimeoutHandler;

/**
 * 
 * @author pabloc
 *
 */
public class RtspClient implements RtspClientInitializer {

	private final EventLoopGroup workerGroup;
	private final RtspHandshakeOperation operation;
	private final SimpleLogger logger;
	private final ServerConfigHolder serverConfig;

	private RtspClientTask rtspTask = null;
	private URI uri;
	
	@Inject
	public RtspClient(SimpleLogger logger,
			@Named("default_worker_group") EventLoopGroup workerGroup,
			ServerConfigHolder serverConfig,
			RtspHandshakeOperation operation) {
		super();
		this.logger = logger;
		this.workerGroup = workerGroup;
		this.serverConfig = serverConfig;
		this.operation = operation;
	}
	
	@Override
	public RtspClientHandle initialize(RtspServerDefinition serverDefinition, RTPServerHandle rtpServer) {
		try {
			this.uri = serverDefinition.getURI();
			rtspTask = new RtspClientTask(rtpServer);
			serverConfig.bind(rtpServer, serverDefinition);
			Thread thread = new Thread(rtspTask);
			thread.setName(serverDefinition.getServerName() + "RTSPClient-" + thread.getName());
			thread.start();
		} catch (URISyntaxException e1) {
			logger.error("Failed to create RTSP Client task", e1);
		}
		return new RtspClientHandleImpl(rtspTask);
	}

	/**
	 * Tarea implementacion actual de cada cliente RTSP
	 * 
	 * @author pabloc
	 *
	 */
	class RtspClientTask implements Runnable { 
		
		private Bootstrap bootstrap;
		private RTPServerHandle rtpServer;
		private boolean connected = false;
		private ChannelPipeline pipeline;

		public RtspClientTask(RTPServerHandle rtpServer) {
			super();
			this.rtpServer = rtpServer;
		}

		@Override
		public void run() {
			int run = 0;
			ChannelFuture future = null;
			try {
				while (future == null && !connected) {
					logger.info("Try Connection. RUN No. {}", ++run);
					future = tryConnect();
					if (future == null || !connected) {
						Thread.sleep(1000 * 10);
					}
				}
				boolean reconnect = false;
				int count = 0;
				while (true) { //The thread remains active checking if it remains receiving data
					logger.trace("Checking connection status for server {}. Receiving? {}", uri, rtpServer.receiving());
					if (rtpServer.receiving()) {
						long secondsSinceLastPacket = rtpServer.getTimeSinceLastPacket(ChronoUnit.SECONDS);
						if (logger.isTraceEnabled()) {
							logger.trace("Last received packet {} seconds ago", secondsSinceLastPacket);
						}
						if (secondsSinceLastPacket > 15L) {
							reconnect = true;
							break;
						} else if (secondsSinceLastPacket >= 0) {
							Thread.sleep(1000 * 10);
						} else {
							logger.info("RTPServer is receiving but no last packet info got");
							if (++count > 2) {
								reconnect = true;
								break;
							}
							Thread.sleep(1000 * 5);
						}
					} else {
						Thread.sleep(1000L);
					}
				}
				if (reconnect) {
					logger.info("Lost connection to server {}. Trying to reconnect", uri);
					rtpServer.shutdown();
					operation.restart();
					connected = false;
					run();
				}
			} catch (InterruptedException e) {
				logger.error("InterruptedException waiting for channel close. RTSP Server was connected? {}", e, connected);
				if (!connected) {
					run();
				} else {
					rtpServer.shutdown();
					operation.restart();
					connected = false;
					run();
				}
			}
		}
		
		private ChannelFuture tryConnect() {
			bootstrap = new Bootstrap();
			bootstrap.group(workerGroup);
			bootstrap.channel(NioSocketChannel.class);
			bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
			bootstrap.handler(new ChannelInitializer<Channel>() {
				
				@Override
				protected void initChannel(Channel ch) throws Exception {
					logger.info("RTSP Client Channel Init");
					pipeline = ch.pipeline();
					ch.pipeline().addLast(new WriteTimeoutHandler(10));
					ch.pipeline().addLast("decoder", new RtspDecoder());
					ch.pipeline().addLast("encoder", new RtspEncoder());
					ch.pipeline().addLast(new HttpObjectAggregator(65536));
					ch.pipeline().addLast("handler", operation);
				}
			});
			try {
				
				ChannelFuture future = bootstrap.connect(uri.getHost(), uri.getPort()).sync();
				future.channel().closeFuture().addListener(closeFuture -> {
					logger.info("Operation Complete: Channel closed");
					if (!closeFuture.isSuccess()) {
						logger.error("RTSP Channel Closed [{}]", closeFuture.cause(), uri);
					}
				});
				// Aca estoy conectado, comienzo chain
				operation.start(rtpServer.serverDefinition(), future.channel());
				connected = true;
				return future;
			} catch (Exception e) {
				logger.error("Failed to stablish a connection with rtsp server {}", e, uri);
				connected = false;
				if (pipeline != null) {
					try {
						logger.debug("Closing PIPELINE");
						pipeline.close().sync();
					} catch (InterruptedException e1) {
						return null;
					}
				}
				return null;
			}
		}
	}

	@Override
	public Integer currentSequence() {
		if (rtspTask != null) {
			return operation.currentSequence();
		}
		return 1;
	}
}
