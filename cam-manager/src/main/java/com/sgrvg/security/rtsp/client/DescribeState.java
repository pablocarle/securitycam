package com.sgrvg.security.rtsp.client;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.util.AsciiString;

/**
 * TODO Deberia procesar SDP?
 * 
 * @author pabloc
 *
 */
public class DescribeState extends RtspHandshakeState {

	private String server;
	private String contentType;
	private Integer contentLength;
	private String date;
	private String contentBase;
	
	
	public DescribeState(URI uri, int sequence, HttpResponse message) {
		super(uri, sequence, message);
		server = message.headers().get(RtspHeaderNames.SERVER);
		contentType = message.headers().get(RtspHeaderNames.CONTENT_TYPE);
		contentLength = message.headers().getInt(RtspHeaderNames.CONTENT_LENGTH);
		date = message.headers().get(RtspHeaderNames.DATE);
		contentBase = message.headers().get(RtspHeaderNames.CONTENT_BASE);
	}

	@Override
	protected Map<AsciiString, Object> doGetState() {
		Map<AsciiString, Object> map = new HashMap<>();
		map.put(RtspHeaderNames.SERVER, server);
		map.put(RtspHeaderNames.CONTENT_TYPE, contentType);
		map.put(RtspHeaderNames.CONTENT_LENGTH, contentLength);
		map.put(RtspHeaderNames.DATE, date);
		map.put(RtspHeaderNames.CONTENT_BASE, contentBase);
		return Collections.unmodifiableMap(map);
	}
}
