package com.sgrvg.security.rtsp.client;

import java.net.URI;
import java.net.URISyntaxException;

import com.google.inject.Inject;
import com.sgrvg.security.SimpleLogger;
import com.sgrvg.security.rtp.server.RTPServerHandle;
import com.sgrvg.security.rtsp.RtspServerDefinition;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.rtsp.RtspDecoder;
import io.netty.handler.codec.rtsp.RtspEncoder;

/**
 * Deberia crear esta clase
 * 
 * TODO Falta manejo de caidas de la conexion. Volver a intentar en N tiempo?
 * 
 * @author pabloc
 *
 */
public class RtspClient implements RtspClientInitializer {

	private RtspClientTask rtspTask = null;
	private EventLoopGroup workerGroup;
	private URI uri;
	private SimpleLogger logger;
	
	@Inject
	public RtspClient(SimpleLogger logger, EventLoopGroup workerGroup) {
		super();
		this.logger = logger;
		this.workerGroup = workerGroup;
	}
	
	@Override
	public RtspClientHandle initialize(RtspServerDefinition serverDefinition, RTPServerHandle rtpServer) {
		try {
			this.uri = serverDefinition.getURI();
			rtspTask = new RtspClientTask(rtpServer);
			Thread thread = new Thread(rtspTask);
			thread.start();
		} catch (URISyntaxException e1) {
			System.err.println("Failed to create rtsp client task");
			e1.printStackTrace();
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
		
		private RtspHandshakeOperation operation;
		private Bootstrap bootstrap = new Bootstrap();
		private RTPServerHandle rtpServer;

		public RtspClientTask(RTPServerHandle rtpServer) {
			super();
			this.operation = null;//new RtspHandshakeOperation(uri);
			this.rtpServer = rtpServer;
		}

		@Override
		public void run() {
			try {
				bootstrap.group(workerGroup);
				bootstrap.channel(NioSocketChannel.class);
				bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
				bootstrap.handler(new ChannelInitializer<Channel>() {

					@Override
					protected void initChannel(Channel ch) throws Exception {
						logger.info("RTSP Client Channel Init");
						ch.pipeline().addLast("decoder", new RtspDecoder());
						ch.pipeline().addLast("encoder", new RtspEncoder());
						ch.pipeline().addLast(new HttpObjectAggregator(65536));
						ch.pipeline().addLast("handler", operation);
					}
				});
				
				ChannelFuture future = bootstrap.connect(uri.getHost(), uri.getPort()).sync();
				future.channel().closeFuture().addListener(closeFuture -> {
					logger.info("Operation Complete: Channel closed");
					if (!closeFuture.isSuccess()) {
						closeFuture.cause().printStackTrace();
					}
				});
				// Aca estoy conectado, comienzo chain
				operation.start(rtpServer.serverDefinition(), future.channel());
				future.channel().closeFuture().sync();
				logger.info("Channel closed");
			} catch (Exception e) {
				logger.error("Failed to stablish a connection with rtsp server {}", e, uri);
			}
		}
	}

	@Override
	public Integer currentSequence() {
		if (rtspTask != null) {
			return rtspTask.operation.currentSequence();
		}
		return 1;
	}
}
