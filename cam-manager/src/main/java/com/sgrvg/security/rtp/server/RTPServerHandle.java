package com.sgrvg.security.rtp.server;

public interface RTPServerHandle {

	RTPServerDefinition serverDefinition();
	
	void waitConnected() throws InterruptedException;
	
}
