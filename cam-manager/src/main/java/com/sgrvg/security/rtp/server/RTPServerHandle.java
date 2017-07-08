package com.sgrvg.security.rtp.server;

import java.net.URISyntaxException;

public interface RTPServerHandle {

	String getID();
	
	RTPServerDefinition serverDefinition() throws URISyntaxException;
	
	void waitConnected() throws InterruptedException, RTPServerInitializationException;
	
}
