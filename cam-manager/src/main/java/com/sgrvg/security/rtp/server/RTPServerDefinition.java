package com.sgrvg.security.rtp.server;

import java.net.URI;

public class RTPServerDefinition {

	private int port;
	private URI rtspServerUri;
	
	public RTPServerDefinition(int port, URI uri) {
		super();
		this.port = port;
		this.rtspServerUri = uri;
	}
	
	public int getPort() {
		return port;
	}
	
	public URI getRtspServerURI() {
		return this.rtspServerUri;
	}
}
