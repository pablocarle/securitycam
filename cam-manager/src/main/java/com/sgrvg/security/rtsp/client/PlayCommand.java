package com.sgrvg.security.rtsp.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.handler.codec.rtsp.RtspMethods;
import io.netty.handler.codec.rtsp.RtspVersions;

public class PlayCommand extends RtspHandshake {

	public PlayCommand(Channel channel, RtspHandshakeState handshakeState) {
		super(channel, handshakeState);
	}

	@Override
	public ChannelFuture call() throws Exception {
		HttpRequest request = new DefaultHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.SETUP, handshakeState.getUri().toASCIIString());
		request.headers().set(RtspHeaderNames.CSEQ, handshakeState.getSequence() + 1);
		request.headers().set(RtspHeaderNames.USER_AGENT, handshakeState.getUserAgent());
		request.headers().set(RtspHeaderNames.RANGE, "npt=0.000-");
		request.headers().set(RtspHeaderNames.SESSION, parseSession(String.valueOf(handshakeState.state().get(RtspHeaderNames.SESSION))));
		ChannelFuture future = channel.write(request);
		channel.flush();
		return future;
	}

	private String parseSession(String value) {
		if (value != null && !value.trim().isEmpty()) {
			String[] tokens = value.split(";");
			if (tokens.length > 0) {
				return tokens[0];
			}
		}
		return null;
	}

	@Override
	public HttpMethod getRtspMethod() {
		return RtspMethods.PLAY;
	}
}
