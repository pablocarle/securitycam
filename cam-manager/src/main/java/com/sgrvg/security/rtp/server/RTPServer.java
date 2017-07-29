package com.sgrvg.security.rtp.server;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sgrvg.security.ServerConfigHolder;
import com.sgrvg.security.SimpleLogger;
import com.sgrvg.security.rtsp.RtspServerDefinition;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;

/**
 * RTP Server implementation. Handles startup of rtp server
 * 
 * @author pabloc
 *
 */
public class RTPServer implements RTPServerInitializer {

	// Deps
	private SimpleLogger logger;
	private EventLoopGroup bossLoopGroup;
	private ServerConfigHolder serverConfig;
	private RTPPacketHandler rtpPacketHandler;
	
	// State
	private RTPServerTask task;
	private RtspServerDefinition server;
	
	@Inject
	public RTPServer(SimpleLogger logger, 
			@Named("rtp_server_boss") EventLoopGroup bossLoopGroup,
			ServerConfigHolder serverConfig,
			RTPPacketHandler rtpPacketHandler) {
		super();
		this.logger = logger;
		this.bossLoopGroup = bossLoopGroup;
		this.serverConfig = serverConfig;
		this.rtpPacketHandler = rtpPacketHandler;
	}
	
	@Override
	public RTPServerHandle initialize(RtspServerDefinition server) {
		this.server = server;
		task = new RTPServerTask();
		Thread thread = new Thread(task);
		thread.start();
		RTPServerHandle handle = new RTPServerHandleImpl(server.getServerName(), task, server);
		serverConfig.bind(handle, rtpPacketHandler);
		return handle;
	}
	
	/**
	 * Tarea principal de cada servidor RTP
	 * 
	 * @author pabloc
	 *
	 */
	class RTPServerTask implements Runnable {

		private Bootstrap bootstrap = new Bootstrap();
		private List<RTPConnectionStateListener> listeners;
		private int port = serverConfig.getNextPortInRange();
		private volatile boolean successfulConnection = false;
		private volatile boolean failedConnection = false;
		private volatile boolean shutingdown = false;
		
		void addConnectionStateListener(RTPConnectionStateListener listener) {
			listeners.add(listener);
		}
		
		@Override
		public void run() {
			bootstrap.group(bossLoopGroup);
			bootstrap.channel(NioDatagramChannel.class);
			bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
			bootstrap.handler(new ChannelInitializer<DatagramChannel>() {

				@Override
				protected void initChannel(DatagramChannel ch) throws Exception {
					logger.info("RTP Server Channel Init");
					ch.pipeline().addLast(rtpPacketHandler);
				}
			});
			
			try {
				ChannelFuture future = bootstrap.bind(port).sync();
				logger.info("Ended bind sync of RTP Server");
				future.addListener(listener -> {
					if (listener.isSuccess()) {
						logger.info("Success server bootstrap bind");
						successfulConnection = true;
						notifySuccessfulConnection();
					} else {
						logger.warn("Failed server bootstrap bind");
						failedConnection = true;
						notifyFailedConnection();
					}
				});
				future.channel().closeFuture().addListener(new ChannelFutureListener() {
					
					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						logger.info("RTP Server Channel closed");
					}
				});
				logger.debug("RTPServer {} channel is open? {}, active? {}", server, future.channel().isOpen(), future.channel().isActive());
				long time = -1L;
				while (future.channel().isOpen() || future.channel().isActive()) {
					if (logger.isDebugEnabled()) {
						//logger.debug("Check connection status of RTP Server {}", server);
					}
					if (shutingdown) {
						logger.debug("Detected shutdown procedure of {} server", server.getServerName());
						time = rtpPacketHandler.getMsSinceLastPacket();
						if (time < (15 * 1000)) { //TODO Config
							logger.debug("Detected reconnection of {} server", server.getServerName());
							shutingdown = false;
						}
					}
					Thread.sleep(500);
				}
				future.channel().closeFuture().sync();
			} catch (Exception e) {
				logger.error("Failed to initialize RTP Server", e);
				failedConnection = true;
				notifyFailedConnection();
			} finally {
				logger.info("Finally RTP Server");
			}
		}
		
		private void notifySuccessfulConnection() {
			if (listeners != null) {
				listeners.forEach(x -> x.notifyState(new ConnectionStateEvent("connected")));
			}
		}
		
		private void notifyFailedConnection() {
			if (listeners != null) {
				listeners.forEach(x -> x.notifyState(new ConnectionStateEvent("failed")));
			}
		}
		
		boolean isSuccessfulConnection() {
			return successfulConnection;
		}
		
		boolean isFailedConnection() {
			return failedConnection;
		}

		public int getAssignedPort() {
			return port;
		}
		
		void shutdown() {
			//En algun momento alguien va a llamar a esto y entrara en shutdown (no esta
			// recibiendo datos
			//Se supone que seguimos conectados
			rtpPacketHandler.restart();
			shutingdown = true;
		}

		Optional<Instant> getLastReceivedPacket() {
			return rtpPacketHandler.getLastTimePacketReceived();
		}

		boolean isShutdown() {
			return shutingdown;
		}
	}
}
