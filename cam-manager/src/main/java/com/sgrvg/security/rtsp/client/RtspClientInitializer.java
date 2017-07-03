package com.sgrvg.security.rtsp.client;

import com.sgrvg.security.rtp.server.RTPServerHandle;
import com.sgrvg.security.rtsp.RtspServerDefinition;

public interface RtspClientInitializer {

	RtspClientHandle initialize(RtspServerDefinition serverDefinition, RTPServerHandle rtpServer);

	Integer currentSequence();
	
}
