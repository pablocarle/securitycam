package com.sgrvg.security;

import java.util.Optional;

import com.sgrvg.security.rtp.server.RTPPacketHandler;
import com.sgrvg.security.rtp.server.RTPServerHandle;
import com.sgrvg.security.rtsp.RtspServerDefinition;

public interface ServerConfigHolder {

	void bind(RTPServerHandle rtpServer, RtspServerDefinition rtspServer);

	void bind(RTPServerHandle handle, RTPPacketHandler rtpPacketHandler);

	Optional<RtspServerDefinition> getRtspEndpoint(RTPPacketHandler rtpPacketHandler);
	
}
