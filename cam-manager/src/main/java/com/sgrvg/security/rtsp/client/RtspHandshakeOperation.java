package com.sgrvg.security.rtsp.client;

import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

import com.sgrvg.security.SimpleLogger;
import com.sgrvg.security.rtp.server.RTPServerDefinition;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;

public class RtspHandshakeOperation extends SimpleChannelInboundHandler<HttpObject> {

	/*
	 * Supported rtsp methods
	 * */
	public static final String OPTIONS = "OPTIONS";
	public static final String DESCRIBE = "DESCRIBE";
	public static final String PLAY = "PLAY";
	public static final String SETUP = "SETUP";
	public static final String TEARDOWN = "TEARDOWN";

	public static final String USER_AGENT = "SGRVG CAM PROJECT";

	private int sequence = 1;
	private RtspHandshake lastCommand = null;
	private URI uri;
	private SimpleLogger logger;
	private RTPServerDefinition rtpServer;
	
	private OptionsState optionsState;
	private DescribeState describeState;
	private SetupState setupState;
	private PlayState playState;
	
	private Function<Void, Void> connectionCompleteFunction;

	public RtspHandshakeOperation(
			SimpleLogger logger) {
		super();
		this.logger = logger;
	}

	public void start(RTPServerDefinition rtpServerDefinition, Channel channel) throws Exception {
		this.uri = rtpServerDefinition.getRtspServerURI();
		this.lastCommand = new OptionsCommand(channel, new OptionsState(uri, sequence));
		this.rtpServer = rtpServerDefinition;
		ChannelFuture future = lastCommand.call();
		future.addListener(fut -> {
			logger.info("{} operation ended with {} status", lastCommand.getRtspMethod().name(), fut.isSuccess());
			if (!fut.isSuccess()) {
				fut.cause().printStackTrace();
			}
		});
		future.sync();
	}
	
	public void shutdown() throws Exception {
		// TODO Teardown + shutdown gracefully
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		logger.info("Channel Read Complete");
		super.channelReadComplete(ctx);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.info("Channel Active");
		super.channelActive(ctx);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.warn("EXCEPTION CAUGHT", cause);
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
				optionsState = new OptionsState(uri, lastCommand.getState().getSequence() + 1, response);
				next = Optional.of(new DescribeCommand(ctx, optionsState));
				break;
			}
			case DESCRIBE: {
				next = Optional.of(prepareSetup(ctx.channel(), response));
				break;
			}
			case SETUP: {
				setupState = new SetupState(uri, lastCommand.getState().getSequence() + 1, response);
				next = Optional.of(new PlayCommand(ctx.channel(), setupState));
				break;
			}
			case PLAY: {
				playState = new PlayState(uri, lastCommand.getState().getSequence() + 1, response);
				next = Optional.of(new TeardownCommand(ctx.channel(), playState));
				if (connectionCompleteFunction != null) {
					connectionCompleteFunction.apply(null); // TODO Alguien que represente el estado de configuracion del server
				}
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
					logger.info("{} operation ended with {} status", nextCommand.getRtspMethod().name(), future.isSuccess());
					if (!future.isSuccess()) {
						future.cause().printStackTrace();
					}
					lastCommand = nextCommand;
				}
			});
			future.sync();
		}
	}

	/**
	 * Solicita inicializacion de servidor RTP
	 * 
	 * @param channel
	 * @param response
	 * @return
	 * @throws RtspHandshakeException 
	 */
	private RtspHandshake prepareSetup(Channel channel, HttpResponse response) throws RtspHandshakeException {
		describeState = new DescribeState(uri, lastCommand.getState().getSequence() + 1, response);
		return new SetupCommand(channel, describeState, rtpServer.getPort());
	}

	public Integer currentSequence() {
		return sequence;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
		if (msg instanceof HttpResponse) {
			HttpResponse response = (HttpResponse) msg;
			if (response.status().equals(HttpResponseStatus.OK)) {
				logger.info("{} received 200 OK", lastCommand.getRtspMethod().name());
				responseOk(ctx, response);
			} else {
				throw new RtspHandshakeException("Couldn't connect to server. Returned with status " + response.status());
			}
		}
		if (msg instanceof HttpContent) {
			HttpContent content = (HttpContent) msg;
			logger.info(content.content().toString(CharsetUtil.UTF_8));

			if (content instanceof LastHttpContent) {
				logger.info("} END OF CONTENT");
			}
		}
	}
}
