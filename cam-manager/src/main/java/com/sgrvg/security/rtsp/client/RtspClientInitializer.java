package com.sgrvg.security.rtsp.client;

import com.sgrvg.security.rtsp.RtspServerDefinition;

public interface RtspClientInitializer {

	RtspClientHandle initialize(RtspServerDefinition serverDefinition);

	Integer currentSequence();
	
}
