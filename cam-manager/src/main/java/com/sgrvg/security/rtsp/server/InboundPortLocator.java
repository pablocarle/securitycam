package com.sgrvg.security.rtsp.server;

import java.util.Set;

public class InboundPortLocator {

	private static InboundPortLocator INSTANCE;
	
	public static InboundPortLocator getPortLocator() {
		if (INSTANCE == null) {
			INSTANCE = new InboundPortLocator();
		}
		return INSTANCE;
	}
	
	private Set<Integer> assignedPorts;
	
	private InboundPortLocator() {
		super();
	}
	
	
	public int getPort() {
		// TODO ThreadLocalRandom + lista?
		return 0;
	}
	
}
