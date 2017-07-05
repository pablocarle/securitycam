package com.sgrvg.security.rtp.server;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sgrvg.security.SimpleLogger;
import com.sgrvg.security.rtsp.RtspServerDefinition;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class RTPServer implements RTPServerInitializer {

	private SimpleLogger logger;
	private EventLoopGroup workerLoopGroup;
	private EventLoopGroup bossLoopGroup;

	private RTPServerTask task;
	private RtpPacketHandler packetHandler;
	
	@Inject
	public RTPServer(SimpleLogger logger, 
			@Named("rtp_server_worker") EventLoopGroup workerLoopGroup,
			@Named("rtp_server_boss") EventLoopGroup bossLoopGroup,
			RtpPacketHandler packetHandler) {
		super();
		this.logger = logger;
		this.packetHandler = packetHandler;
		this.workerLoopGroup = workerLoopGroup;
		this.bossLoopGroup = bossLoopGroup;
	}
	
	@Override
	public RTPServerHandle initialize(RtspServerDefinition server) {
		task = new RTPServerTask();
		Thread thread = new Thread(task);
		thread.start();
		return new RTPServerHandleImpl(task, server);
	}
	
	/**
	 * Tarea principal de cada servidor RTP
	 * 
	 * @author pabloc
	 *
	 */
	class RTPServerTask implements Runnable {

		private ServerBootstrap bootstrap = new ServerBootstrap();
		private List<RTPConnectionStateListener> listeners;
		private volatile boolean successfulConnection = false;
		private volatile boolean failedConnection = false;
		private int port = 35678;
		
		void addConnectionStateListener(RTPConnectionStateListener listener) {
			listeners.add(listener);
		}
		
		@Override
		public void run() {
			bootstrap.group(bossLoopGroup, workerLoopGroup);
			bootstrap.channel(NioServerSocketChannel.class);
			bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					logger.info("RTP Server Channel Init");
					ch.pipeline().addLast(packetHandler);
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
	}
}
