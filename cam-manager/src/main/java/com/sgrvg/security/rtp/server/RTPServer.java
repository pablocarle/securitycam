package com.sgrvg.security.rtp.server;

import java.util.List;

import com.google.inject.Inject;
import com.sgrvg.security.SimpleLogger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class RTPServer implements RTPServerInitializer {

	private SimpleLogger logger;
	private EventLoopGroup workerGroup;

	private RTPServerTask task;
	
	@Inject
	public RTPServer(SimpleLogger logger, EventLoopGroup workerGroup) {
		super();
		this.logger = logger;
		this.workerGroup = workerGroup;
	}
	
	@Override
	public RTPServerHandle initialize() {
		task = new RTPServerTask();
		Thread thread = new Thread(task);
		thread.start();
		return new RTPServerHandleImpl(task);
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
		private int port;
		
		void addConnectionStateListener(RTPConnectionStateListener listener) {
			listeners.add(listener);
		}
		
		@Override
		public void run() {
			bootstrap.group(workerGroup);
			bootstrap.channel(NioServerSocketChannel.class);
			bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
			bootstrap.handler(new ChannelInitializer<Channel>() {

				@Override
				protected void initChannel(Channel ch) throws Exception {
					logger.info("RTP Server Channel Init");
					ch.pipeline().addLast(new RtpPacketHandler());
				}
			});
			
			try {
				ChannelFuture future = bootstrap.bind().sync();
				future.addListener(listener -> {
					if (listener.isSuccess()) {
						successfulConnection = true;
						notifySuccessfulConnection();
					} else {
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
			} catch (InterruptedException e) {
				logger.error("Failed to initialize RTP Server", e);
				failedConnection = true;
				notifyFailedConnection();
			}
		}
		
		private void notifySuccessfulConnection() {
			listeners.forEach(x -> x.notifyState(new ConnectionStateEvent("connected")));
		}
		
		private void notifyFailedConnection() {
			listeners.forEach(x -> x.notifyState(new ConnectionStateEvent("failed")));
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
