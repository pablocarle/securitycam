package com.sgrvg.security.rtsp.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.rtsp.RtspMethods;
import io.netty.handler.codec.rtsp.RtspVersions;

/**
 * Informa
 * 
 * @author pabloc
 *
 */
public class SetupCommand extends RtspHandshake {

	private Integer port;
	
	public SetupCommand(Channel channel, RtspHandshakeState handshakeState, Integer port) {
		super(channel, handshakeState);
		this.port = port;
	}

	@Override
	public ChannelFuture call() throws Exception {
		HttpRequest request = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.SETUP, handshakeState.getUri().toASCIIString() + "/trackID=0");
		request.headers().set("CSeq", handshakeState.getSequence() + 1);
		request.headers().set("User-Agent", RtspHandshakeState.USER_AGENT);
		request.headers().set("Transport", "RTP/AVP;unicast;client_port=" + port + "-" + (port + 1));
		return channel.writeAndFlush(request);
	}

	@Override
	public HttpMethod getRtspMethod() {
		return RtspMethods.SETUP;
	}
}
