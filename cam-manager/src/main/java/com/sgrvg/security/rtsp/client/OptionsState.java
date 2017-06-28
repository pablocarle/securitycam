package com.sgrvg.security.rtsp.client;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import io.netty.handler.codec.http.DefaultHttpMessage;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.util.AsciiString;

public class OptionsState extends RtspHandshakeState {

	private String options;
	
	public OptionsState(URI uri, String userAgent, int sequence) {
		super(uri, userAgent, sequence);
	}

	public OptionsState(URI uri, DefaultHttpMessage message) {
		super(uri, message);
		this.options = message.headers().get(RtspHeaderNames.PUBLIC);
	}

	@Override
	protected Map<AsciiString, Object> doGetState() {
		return Collections.singletonMap(RtspHeaderNames.PUBLIC, options);
	}
}
