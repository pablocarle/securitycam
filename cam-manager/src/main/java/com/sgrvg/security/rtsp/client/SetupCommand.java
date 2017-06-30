package com.sgrvg.security.rtsp.client;

import com.sgrvg.security.rtp.server.RTPListener;

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

	private Integer lowPort;
	private Integer highPort;
	
	public SetupCommand(Channel channel, RtspHandshakeState handshakeState, RTPListener listener) {
		super(channel, handshakeState);
		this.lowPort = listener.getLowPort();
		this.highPort = listener.getHighPort();
	}

	@Override
	public ChannelFuture call() throws Exception {
		HttpRequest request = new DefaultFullHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.SETUP, handshakeState.getUri().toASCIIString() + "/trackID=0");
		request.headers().set("CSeq", handshakeState.getSequence() + 1);
		request.headers().set("User-Agent", RtspHandshakeState.USER_AGENT);
		request.headers().set("Transport", "RTP/AVP;unicast;client_port=" + lowPort + "-" + highPort);
		return channel.writeAndFlush(request);
	}

	@Override
	public HttpMethod getRtspMethod() {
		return RtspMethods.SETUP;
	}
}
