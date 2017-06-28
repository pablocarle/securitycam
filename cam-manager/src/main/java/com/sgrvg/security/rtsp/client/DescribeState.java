package com.sgrvg.security.rtsp.client;

import java.net.URI;
import java.util.Map;

import io.netty.handler.codec.http.DefaultHttpMessage;
import io.netty.util.AsciiString;

public class DescribeState extends RtspHandshakeState {

	public DescribeState(URI uri, DefaultHttpMessage message) {
		super(uri, message);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Map<AsciiString, Object> doGetState() {
		// TODO Auto-generated method stub
		return null;
	}
}
