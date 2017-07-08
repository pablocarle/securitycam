package com.sgrvg.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import com.sgrvg.security.rtp.server.RTPServerDefinition;
import com.sgrvg.security.rtsp.RtspServerDefinition;

public class ServerConfigHolderImpl implements ServerConfigHolder {

	private Map map = new ConcurrentHashMap<>();
	
	@Inject
	public ServerConfigHolderImpl() {
		super();
	}
	
	@Override
	public RtspServerDefinition getActiveServer(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addActiveServer(RTPServerDefinition rtpServer, String id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void bind(RTPServerDefinition rtpServer, RtspServerDefinition rtspServer) {
		// TODO Auto-generated method stub

	}

}
