package com.sgrvg.security.rtsp.client;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.util.AsciiString;
import io.netty.util.internal.StringUtil;

public abstract class RtspHandshakeState {

	public static final String USER_AGENT = RtspHandshakeOperation.USER_AGENT;
	
	private int sequence;
	private URI uri;

	protected RtspHandshakeState(URI uri, int sequence) {
		super();
		this.sequence = sequence;
		this.uri = uri;
	}
	
	protected RtspHandshakeState(URI uri, HttpResponse message) {
		super();
		String sequenceHeader = message.headers().get(RtspHeaderNames.CSEQ);
		if (!StringUtil.isNullOrEmpty(sequenceHeader)) {
			sequence = Integer.parseInt(sequenceHeader);
		}
		this.uri = uri;
	}
	
	public Map<AsciiString, Object> state() {
		Map<AsciiString, Object> map = new HashMap<>();
		map.put(RtspHeaderNames.CSEQ, String.valueOf(sequence));
		map.put(RtspHeaderNames.USER_AGENT, RtspHandshakeState.USER_AGENT);
		map.putAll(doGetState());
		return map;
	}
	
	protected abstract Map<AsciiString, Object> doGetState();
	
	public int getSequence() {
		return sequence;
	}
	
	public URI getUri() {
		return uri;
	}
}
