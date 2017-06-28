package com.sgrvg.security;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.rtsp.RtspEncoder;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.handler.codec.rtsp.RtspMethods;
import io.netty.handler.codec.rtsp.RtspVersions;

public class Main {

	// Ejemplos

	private static final String CAM_HOST = "192.168.10.105";
	private static final int CAM_PORT = 554;

	public static void main(String[] args) throws Exception {
		
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		URI uri = new URI("rtsp://" + CAM_HOST + ":" + CAM_PORT + "/12");
		
		try {
			Bootstrap bootstrap = new Bootstrap();
			bootstrap.group(workerGroup);
			bootstrap.channel(NioSocketChannel.class);
			bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
			bootstrap.handler(new ChannelInitializer<Channel>() {

				@Override
				protected void initChannel(Channel ch) throws Exception {
					System.out.println("init channel");
					ch.pipeline().addLast(new RtspEncoder());
				}
			});
			/// TODO Mirar el ejemplo en run() que hace envio de mensaje, deberia armar la cadena de mensajes
			//Start channel
			ChannelFuture f = bootstrap.connect(CAM_HOST, CAM_PORT).sync();
			System.out.println("connected");
			
			HttpRequest request = new DefaultHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.SETUP, uri.toASCIIString());
			request.headers().set(RtspHeaderNames.CSEQ, "1");
			request.headers().set(RtspHeaderNames.USER_AGENT, "cam-manager 1.0");
			request.headers().set(RtspHeaderNames.TRANSPORT, "RTP/AVP;unicast;client_port=32456-32457");
			f.channel().write(request).addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					System.out.println("Channel future complete with status " + future.isSuccess());
					
					if (!future.isSuccess()) {
						System.err.println(future.cause());
					}
				}
			});
			f.channel().flush();
			
			System.out.println("sent setup request");
			
			f.channel().closeFuture().sync();
			System.out.println("closed");
			
		} finally {
			workerGroup.shutdownGracefully();
		}
	}

	public void run() throws URISyntaxException {
		URI uri = new URI("rtsp://" + CAM_HOST + ":" + CAM_PORT + "/12");
		Bootstrap bootstrap = null;//= createBootstrap(new Bootstrap(), loop);
		ChannelFuture future = bootstrap.connect(new InetSocketAddress(CAM_HOST, CAM_PORT));
		future.addListener(new CameraConnectionListener());
		Channel channel = future.awaitUninterruptibly().channel();
		if (!future.isSuccess()) {
			future.cause().printStackTrace();
			return;
		}
		HttpRequest request = new DefaultHttpRequest(
				HttpVersion.HTTP_1_1, RtspMethods.PLAY, uri.toASCIIString());
		
		request.headers().set(HttpHeaderNames.HOST, uri.getHost());
		request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
		request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
		
		channel.write(request);
		
		channel.closeFuture().awaitUninterruptibly().addListener(new ChannelFutureListener() {
			
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				System.out.println("Connection closed");
			}
		});
	}

	public Bootstrap createBootstrap(Bootstrap bootstrap, EventLoopGroup eventLoop) {  
		if (bootstrap != null) {  
			final CameraInboundHandler cameraInboundHandler = new CameraInboundHandler();
			bootstrap.group(eventLoop);  
			bootstrap.channel(NioSocketChannel.class);  
			bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
			bootstrap.handler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ch.pipeline().addLast(cameraInboundHandler);
				}
			});
			bootstrap.remoteAddress(CAM_HOST, CAM_PORT);
		}  
		return bootstrap;  
	} 
}
