package com.sgrvg.security.rtsp.client;

public class RtspHandshakeException extends Exception {

	public RtspHandshakeException(String message) {
		super(message);
	}

	public RtspHandshakeException(String message, Throwable e) {
		super(message, e);
	}

	private static final long serialVersionUID = 1L;

}
