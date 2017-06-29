package com.sgrvg.security.rtsp.client;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.netty.handler.codec.http.DefaultHttpMessage;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.util.AsciiString;

public class PlayState extends RtspHandshakeState {

	private String server;
	private String session;
	private String range;
	private String date;
	private String rtpInfo;
	
	protected PlayState(URI uri, DefaultHttpMessage message) {
		super(uri, message);
		this.server = message.headers().get(RtspHeaderNames.SERVER);
		this.session = message.headers().get(RtspHeaderNames.SESSION);
		this.range = message.headers().get(RtspHeaderNames.RANGE);
		this.date = message.headers().get(RtspHeaderNames.DATE);
		this.rtpInfo = message.headers().get(RtspHeaderNames.RTP_INFO);
	}

	@Override
	protected Map<AsciiString, Object> doGetState() {
		Map<AsciiString, Object> map = new HashMap<>();
		map.put(RtspHeaderNames.SERVER, server);
		map.put(RtspHeaderNames.SESSION, session);
		map.put(RtspHeaderNames.RANGE, range);
		map.put(RtspHeaderNames.DATE, date);
		map.put(RtspHeaderNames.RTP_INFO, rtpInfo);
		return Collections.unmodifiableMap(map);
	}
}
