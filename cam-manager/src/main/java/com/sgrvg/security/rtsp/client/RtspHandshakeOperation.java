package com.sgrvg.security.rtsp.client;

import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

import com.google.inject.Inject;
import com.sgrvg.security.ServerConfigHolder;
import com.sgrvg.security.SimpleLogger;
import com.sgrvg.security.rtp.server.RTPServerDefinition;
import com.sgrvg.security.rtsp.RtspServerDefinition;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import io.netty.util.CharsetUtil;

@Sharable
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

	private final ServerConfigHolder serverConfig;
	private final SimpleLogger logger;
	
	private int sequence = 1;
	private RtspHandshake lastCommand = null;
	private URI uri;
	private RTPServerDefinition rtpServer;
	
	private OptionsState optionsState;
	private DescribeState describeState;
	private SetupState setupState;
	private PlayState playState;
	
	private Channel channel;
	
	private Function<Void, Void> connectionCompleteFunction;

	@Inject
	public RtspHandshakeOperation(
			SimpleLogger logger,
			ServerConfigHolder serverConfig) {
		super();
		this.logger = logger;
		this.serverConfig = serverConfig;
	}

	public void start(RTPServerDefinition rtpServerDefinition, Channel channel) throws Exception {
		this.uri = rtpServerDefinition.getRtspServerURI();
		this.lastCommand = new OptionsCommand(channel, new OptionsState(uri, sequence));
		this.rtpServer = rtpServerDefinition;
		ChannelFuture future = lastCommand.call();
		future.addListener(fut -> {
			logger.info("{} operation ended with {} status", lastCommand.getRtspMethod().name(), fut.isSuccess());
			if (!fut.isSuccess()) {
				logger.error("{} operation failed", future.cause(), lastCommand.getRtspMethod().name());
			}
		});
		future.sync();
	}

	void restart() {
		try {
			lastCommand = new TeardownCommand(channel, playState);
			lastCommand.call().sync();
		} catch (Exception e) {
			logger.error("Failed while sending TEARDOWN command", e);
		}
		sequence = 1;
		optionsState = null;
		describeState = null;
		setupState = null;
		playState = null;
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
		if (cause instanceof WriteTimeoutException) {
			// A write operation could not be done in X seconds
			logger.error("Timeout writing to channel", cause);
			throw (WriteTimeoutException)cause;
		} else if (cause instanceof ReadTimeoutException) {
			// No inbound traffic for X seconds
			logger.error("Timeout, no content received for X seconds", cause);
			throw (ReadTimeoutException)cause;
		} else {
			super.exceptionCaught(ctx, cause);
		}
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

		Optional<RtspHandshake> next;
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
				channel = ctx.channel();
				next = Optional.empty();
				break;
			}
			case TEARDOWN: {
				next = Optional.empty();
				logger.info("Received successful TEARDOWN");
				break;
			}
			default: {
				throw new RtspHandshakeException("Unrecognized or unsupported rtsp method " + lastCommand.getRtspMethod().asciiName());
			}
		}
		if (next.isPresent()) {
			final RtspHandshake nextCommand = next.get();
			ChannelFuture future = nextCommand.call();
			future.addListener((ChannelFutureListener) future1 -> {
                logger.info("{} operation ended with {} status", nextCommand.getRtspMethod().name(), future1.isSuccess());
                if (!future1.isSuccess()) {
                    logger.error("{} operation failed", future1.cause(), nextCommand.getRtspMethod().name());
                }
                lastCommand = nextCommand;
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
		Optional<RtspServerDefinition> rtspServer = serverConfig.getRtspEndpoint(rtpServer);
		if (rtspServer.isPresent()) {
			rtspServer.get().getSessionDescription().setPps(describeState.getPPS());
			rtspServer.get().getSessionDescription().setSps(describeState.getSPS());
		} else {
			throw new RtspHandshakeException("Couldn't find bound rtsp endpoint definition");
		}
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
