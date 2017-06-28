package com.sgrvg.security.rtsp.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.HttpMethod;

public class PlayCommand extends RtspHandshake {

	public PlayCommand(Channel channel, RtspHandshakeState handshakeState) {
		super(channel, handshakeState);
	}

	@Override
	public ChannelFuture call() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpMethod getRtspMethod() {
		// TODO Auto-generated method stub
		return null;
	}

}
