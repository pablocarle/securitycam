package com.sgrvg.security.rtsp.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.handler.codec.rtsp.RtspMethods;
import io.netty.handler.codec.rtsp.RtspVersions;

public class DescribeCommand extends RtspHandshake {

	public DescribeCommand(Channel channel, RtspHandshakeState handshakeState) {
		super(channel, handshakeState);
	}

	@Override
	public ChannelFuture call() throws Exception {
		HttpRequest request = new DefaultHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.SETUP, handshakeState.getUri().toASCIIString());
		
		request.headers().set(RtspHeaderNames.CSEQ, String.valueOf(handshakeState.getSequence()));
		request.headers().set(RtspHeaderNames.USER_AGENT, handshakeState.getUserAgent());
		request.headers().set(RtspHeaderNames.ACCEPT, "application/sdp");
		
		ChannelFuture future = channel.write(request);
		channel.flush();
		return future;
	}

	@Override
	public HttpMethod getRtspMethod() {
		return RtspMethods.DESCRIBE;
	}
}
