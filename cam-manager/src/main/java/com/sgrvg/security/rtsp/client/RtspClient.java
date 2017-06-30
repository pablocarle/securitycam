package com.sgrvg.security.rtsp.client;

import java.net.URI;
import java.net.URISyntaxException;

import com.sgrvg.security.rtsp.RtspServerDefinition;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
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

	private RtspClientTask task = null;
	
	@Override
	public RtspClientHandle initialize(RtspServerDefinition serverDefinition) {
		try {
			task = new RtspClientTask(serverDefinition.getURI());
			Thread thread = new Thread(task);
			thread.start();
		} catch (URISyntaxException e1) {
			System.err.println("Failed to create rtsp client task");
			e1.printStackTrace();
		}
		return null;
	}

	private class RtspClientTask implements Runnable { 
		
		private URI uri;
		private RtspHandshakeOperation operation;
		private Bootstrap bootstrap = new Bootstrap();
		private EventLoopGroup workerGroup = new NioEventLoopGroup();;

		public RtspClientTask(URI uri) {
			super();
			this.uri = uri;
			this.operation = new RtspHandshakeOperation(uri);
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
						System.out.println("Channel Init");
						ch.pipeline().addLast("decoder", new RtspDecoder());
						ch.pipeline().addLast("encoder", new RtspEncoder());
						ch.pipeline().addLast(new HttpObjectAggregator(65536));
						ch.pipeline().addLast("handler", operation);
					}
				});
				
				ChannelFuture future = bootstrap.connect(uri.getHost(), uri.getPort()).sync();
				future.channel().closeFuture().addListener(closeFuture -> {
					System.out.println("Operation Complete: Channel closed");
					if (!closeFuture.isSuccess()) {
						closeFuture.cause().printStackTrace();
					}
				});
				// Aca estoy conectado, comienzo chain
				operation.start(RtspClient.this, future.channel());
				future.channel().closeFuture().sync();
				System.out.println("Channel closed");
			} catch (Exception e) {
				System.err.println("Failed to stablish a connection with rtsp server " + uri);
				e.printStackTrace();
			}
		}
	}

	@Override
	public Integer currentSequence() {
		if (task != null) {
			return task.operation.currentSequence();
		}
		return 1;
	}
}
