package com.sgrvg.security.rtp.server;

public class RTPServerHandleImpl implements RTPServerHandle {

	private RTPServerDefinition serverDefinition;

	RTPServerHandleImpl(RTPServerDefinition serverDefinition) {
		super();
		this.serverDefinition = serverDefinition;
	}
	
	@Override
	public RTPServerDefinition serverDefinition() {
		// TODO Auto-generated method stub
		return null;
	}

}
