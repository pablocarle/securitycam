package com.sgrvg.security.rtsp.client;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.util.AsciiString;

public class SetupState extends RtspHandshakeState {

	private String server;
	private String sessionHeader;
	private String sessionNumber;
	private Long timeout;
	private String date;
	private String transport;
	
	protected SetupState(URI uri, int sequence, HttpResponse message) {
		super(uri, sequence, message);
		this.server = message.headers().get(RtspHeaderNames.SERVER);
		this.sessionHeader = message.headers().get(RtspHeaderNames.SESSION);
		this.date = message.headers().get(RtspHeaderNames.DATE);
		this.transport = message.headers().get(RtspHeaderNames.TRANSPORT);
		parseSession();
	}

	private void parseSession() {
		if (sessionHeader != null && !sessionHeader.trim().isEmpty()) {
			String[] tokens = sessionHeader.split(";");
			if (tokens.length > 0) {
				sessionNumber = tokens[0];
			}
			if (tokens.length > 1) {
				timeout = Long.parseLong(tokens[1].replaceFirst("timeout=", "").trim());
			}
		}
	}

	@Override
	protected Map<AsciiString, Object> doGetState() {
		Map<AsciiString, Object> map = new HashMap<>();
		map.put(RtspHeaderNames.SERVER, server);
		map.put(RtspHeaderNames.SESSION, sessionHeader);
		map.put(RtspHeaderNames.DATE, date);
		map.put(RtspHeaderNames.TRANSPORT, transport);
		return Collections.unmodifiableMap(map);
	}

	public String getSessionNumber() {
		return sessionNumber;
	}

	public Long getTimeout() {
		return timeout;
	}
}
