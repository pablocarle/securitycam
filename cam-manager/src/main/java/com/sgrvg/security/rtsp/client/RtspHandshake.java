package com.sgrvg.security.rtsp.client;

import java.util.concurrent.Callable;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.HttpMethod;

public abstract class RtspHandshake implements Callable<ChannelFuture> {
	
	protected Channel channel;
	protected RtspHandshakeState handshakeState;

	public RtspHandshake(Channel channel, RtspHandshakeState handshakeState) {
		super();
		this.channel = channel;
		this.handshakeState = handshakeState;
	}

	public abstract HttpMethod getRtspMethod();
	
	public RtspHandshakeState getState() {
		return handshakeState;
	}
	
	protected Channel getChannel() {
		return channel;
	}
}
