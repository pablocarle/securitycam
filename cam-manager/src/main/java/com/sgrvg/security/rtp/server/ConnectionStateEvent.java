package com.sgrvg.security.rtp.server;

public class ConnectionStateEvent {

	private String event;

	ConnectionStateEvent(String event) {
		super();
		this.event = event;
	}

	public String getEvent() {
		return event;
	}
}
