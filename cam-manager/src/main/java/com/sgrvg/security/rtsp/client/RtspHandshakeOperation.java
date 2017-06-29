package com.sgrvg.security.rtsp.client;

import java.net.URI;
import java.util.Optional;

import com.sgrvg.security.rtp.server.RTPListener;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpResponse;
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
	private HttpMessage lastMessage;
	private RtspClient rtspClient;
	
	public RtspHandshakeOperation(URI uri) {
		super();
		this.uri = uri;
	}
	
	public void start(RtspClient rtspClient, Channel channel) throws Exception {
		this.lastCommand = new OptionsCommand(channel, new OptionsState(uri, sequence));
		this.rtspClient = rtspClient;
		ChannelFuture future = lastCommand.call();
		future.addListener(fut -> {
			System.out.println(lastCommand.getRtspMethod().name() + " operation ended with " + fut.isSuccess() + " status");
			if (!fut.isSuccess()) {
				fut.cause().printStackTrace();
			}
		});
		future.await();
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		System.out.println("channel read complete");
		super.channelReadComplete(ctx);
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		System.out.println("channel read. message type is: " + (msg != null ? msg.getClass().getName() : "null"));
		if (msg instanceof HttpResponse) {
			HttpResponse response = (HttpResponse) msg;
			if (response.status().equals(HttpResponseStatus.OK)) {
				lastMessage = (HttpMessage) msg;
				responseOk(ctx, response);
			} else {
				throw new RtspHandshakeException("Couldn't connect to server"); //TODO Mensajes
			}
		}
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("Channel Active");
		// TODO Auto-generated method stub
		super.channelActive(ctx);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}
	
	/**
	 * Continue processing the handshake
	 * 
	 * @param ctx The channel that handles the socket
	 * @param response The response obtained from the last operation
	 * @throws Exception 
	 */
	private void responseOk(ChannelHandlerContext ctx, HttpResponse response) throws Exception {
		if (lastCommand == null) {
			throw new RtspHandshakeException("Invalid status for receiving a response ok status notification");
		}
		
		Optional<RtspHandshake> next = null;
		switch (lastCommand.getRtspMethod().asciiName().toString().toUpperCase()) {
			case OPTIONS: {
				next = Optional.of(new DescribeCommand(ctx, new OptionsState(uri, response)));
				break;
			}
			case DESCRIBE: {
				next = Optional.of(prepareSetup(ctx.channel(), response));
				break;
			}
			case SETUP: {
				next = Optional.of(new PlayCommand(ctx.channel(), new SetupState(uri, response)));
				break;
			}
			case PLAY: {
				//next = Optional.empty();
				next = Optional.of(new TeardownCommand(ctx.channel(), new PlayState(uri, response)));
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
						future.cause().printStackTrace();
					}
					lastCommand = nextCommand;
				}
			});
			future.await();
		}
	}

	/**
	 * TODO Debe levantar el server RTP
	 * 
	 * @param channel
	 * @param response
	 * @return
	 */
	private RtspHandshake prepareSetup(Channel channel, HttpResponse response) {
		RTPListener listener = new RTPListener();
		//TODO Levantar el RTP
		return new SetupCommand(channel, new DescribeState(uri, response), listener);
	}

	public Integer currentSequence() {
		return sequence;
	}
}
