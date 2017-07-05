package com.sgrvg.security.rtp.server;

import com.sgrvg.security.rtsp.RtspServerDefinition;

public interface RTPServerInitializer {

	/**
	 * It should guarantee that the server is bound to a local network address
	 * by the time the method returns.
	 * 
	 * @param server The RTSP Server from which it will receive data 
	 * 
	 * @return
	 */
	RTPServerHandle initialize(RtspServerDefinition server);
	
}
