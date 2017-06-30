package com.sgrvg.security.rtsp.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.handler.codec.rtsp.RtspMethods;
import io.netty.handler.codec.rtsp.RtspVersions;

public class TeardownCommand extends RtspHandshake {

	public TeardownCommand(Channel channel, RtspHandshakeState handshakeState) {
		super(channel, handshakeState);
	}

	@Override
	public ChannelFuture call() throws Exception {
		HttpRequest request = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0, getRtspMethod(), handshakeState.getUri().toASCIIString());
		request.headers().set("CSeq", handshakeState.getSequence() + 1);
		request.headers().set("User-Agent", RtspHandshakeState.USER_AGENT);
		request.headers().set("Session", handshakeState.state().get(RtspHeaderNames.SESSION));
		return channel.writeAndFlush(request);
	}

	@Override
	public HttpMethod getRtspMethod() {
		return RtspMethods.TEARDOWN;
	}
}
