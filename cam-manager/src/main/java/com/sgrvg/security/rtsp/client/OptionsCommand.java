package com.sgrvg.security.rtsp.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.handler.codec.rtsp.RtspMethods;
import io.netty.handler.codec.rtsp.RtspVersions;

public class OptionsCommand extends RtspHandshake {

	public OptionsCommand(Channel channel, RtspHandshakeState handshakeState) {
		super(channel, handshakeState);
	}

	@Override
	public ChannelFuture call() throws Exception {
		HttpRequest request = new DefaultHttpRequest(RtspVersions.RTSP_1_0, getRtspMethod(), handshakeState.getUri().toASCIIString());
		request.headers().set(RtspHeaderNames.CSEQ, handshakeState.getSequence() + 1);
		request.headers().set(RtspHeaderNames.USER_AGENT, RtspHandshakeState.USER_AGENT);
		return channel.writeAndFlush(request);
	}

	@Override
	public HttpMethod getRtspMethod() {
		return RtspMethods.OPTIONS;
	}
}
