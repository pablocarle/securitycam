package com.sgrvg.security.rtsp.client;

import java.net.URI;
import java.util.Map;

import io.netty.handler.codec.http.DefaultHttpMessage;
import io.netty.util.AsciiString;

public class SetupState extends RtspHandshakeState {

	protected SetupState(URI uri, DefaultHttpMessage message) {
		super(uri, message);
	}

	@Override
	protected Map<AsciiString, Object> doGetState() {
		// TODO Auto-generated method stub
		return null;
	}

}
