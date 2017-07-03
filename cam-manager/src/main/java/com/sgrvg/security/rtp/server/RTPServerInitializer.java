package com.sgrvg.security.rtp.server;

public interface RTPServerInitializer {

	/**
	 * It should guarantee that the server is bound to a local network address
	 * by the time the method returns.
	 * 
	 * @return
	 */
	RTPServerHandle initialize();
	
}
