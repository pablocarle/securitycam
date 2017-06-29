package com.sgrvg.security.rtsp.client;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.util.AsciiString;

public class OptionsState extends RtspHandshakeState {

	private String options;
	
	public OptionsState(URI uri, int sequence) {
		super(uri, sequence);
	}

	public OptionsState(URI uri, HttpResponse message) {
		super(uri, message);
		this.options = message.headers().get(RtspHeaderNames.PUBLIC);
	}

	@Override
	protected Map<AsciiString, Object> doGetState() {
		return Collections.singletonMap(RtspHeaderNames.PUBLIC, options);
	}
}
