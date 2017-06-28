package com.sgrvg.security.rtsp.client;

import java.net.URI;
import java.util.Optional;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

public class RtspHandshakeOperation extends ChannelInboundHandlerAdapter {

	/*
	 * Spported rtsp methods
	 * */
	public static final String OPTIONS = "OPTIONS";
	public static final String DESCRIBE = "DESCRIBE";
	public static final String PLAY = "PLAY";
	public static final String SETUP = "SETUP";
	public static final String TEARDOWN = "TEARDOWN";
	
	public static final String USER_AGENT = "cam-manager 1.0";
	
	private int sequence = 1;
	private RtspHandshake lastCommand = null;
	private URI uri;
	
	public RtspHandshakeOperation(URI uri) {
		super();
		this.uri = uri;
	}
	
	public void start(Channel channel) throws Exception {
		lastCommand = new OptionsCommand(channel, new OptionsState(uri, USER_AGENT, sequence++));
		ChannelFuture future = lastCommand.call();
		future.addListener(new ChannelFutureListener() {
			//TODO Reemplazar estos 2 listeners por un utilitario de logging
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				System.out.println("OPTIONS operation ended with " + future.isSuccess() + " status");
				if (!future.isSuccess()) {
					System.err.println(future.cause());
				}
			}
		});
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		System.out.println("channel read complete");
		super.channelReadComplete(ctx);
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		System.out.println("channel read");
		DefaultFullHttpResponse response = (DefaultFullHttpResponse) msg;
		if (response.status().equals(HttpResponseStatus.OK)) {
			responseOk(ctx.channel(), response);
		} else {
			throw new RtspHandshakeException("Couldn't connect to server"); //TODO Mensajes
		}
	}
	
	private void responseOk(Channel channel, DefaultFullHttpResponse response) throws Exception {
		if (lastCommand == null) {
			throw new RtspHandshakeException("Invalid status for receiving a response ok status notification");
		}
		
		Optional<RtspHandshake> next = null;
		switch (lastCommand.getRtspMethod().asciiName().toString().toUpperCase()) {
			case OPTIONS: {
				next = Optional.of(new DescribeCommand(channel, new OptionsState(uri, response)));
				break;
			}
			case DESCRIBE: {
				next = Optional.of(new SetupCommand(channel, new DescribeState(uri, response)));
				break;
			}
			case SETUP: {
				next = Optional.of(new PlayCommand(channel, new SetupState(uri, response)));
				break;
			}
			case PLAY: {
				next = Optional.empty();
				break;
			}
			case TEARDOWN: {
				next = Optional.empty();
				break;
			}
			default: {
				throw new RtspHandshakeException("Unrecognized or unsupported rtsp method " + lastCommand.getRtspMethod().asciiName());
			}
		}
		if (next.isPresent()) {
			final RtspHandshake nextCommand = next.get();
			ChannelFuture future = nextCommand.call();
			future.addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					System.out.println(nextCommand.getRtspMethod().name() + " operation ended with " + future.isSuccess() + " status");
					if (!future.isSuccess()) {
						System.err.println(future.cause());
					}
					lastCommand = nextCommand;
				}
			});
		}
	}

	public Integer currentSequence() {
		return sequence;
	}
}
