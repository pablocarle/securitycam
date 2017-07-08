package com.sgrvg.security;

import com.sgrvg.security.rtp.server.RTPServerDefinition;
import com.sgrvg.security.rtsp.RtspServerDefinition;

public interface ServerConfigHolder {

	RtspServerDefinition getActiveServer(String id);
	
	void addActiveServer(RTPServerDefinition rtpServer, String id);
	
	void bind(RTPServerDefinition rtpServer, RtspServerDefinition rtspServer);
	
}
