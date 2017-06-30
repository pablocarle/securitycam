package com.sgrvg.security.rtsp.client;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.rtsp.RtspMethods;
import io.netty.handler.codec.rtsp.RtspVersions;

public class DescribeCommand extends RtspHandshake {

	private ChannelHandlerContext channelHandler;
	
	public DescribeCommand(ChannelHandlerContext channelHandler, RtspHandshakeState handshakeState) {
		super(channelHandler.channel(), handshakeState);
		this.channelHandler = channelHandler;
	}

	@Override
	public ChannelFuture call() throws Exception {
		HttpRequest request = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0, getRtspMethod(), handshakeState.getUri().toASCIIString());
		
		request.headers().set("CSeq", handshakeState.getSequence() + 1);
		request.headers().set("User-Agent", RtspHandshakeState.USER_AGENT);
		request.headers().set("Accept", "application/sdp");
		
		return channelHandler.writeAndFlush(request);
	}

	@Override
	public HttpMethod getRtspMethod() {
		return RtspMethods.DESCRIBE;
	}
}
