package com.sgrvg.security.rtsp;

import java.net.URI;
import java.net.URISyntaxException;

public class RtspServerDefinition {
	
	private String host;
	private int port;
	private String endpoint;
	
	public RtspServerDefinition(String host, int port, String endpoint) {
		super();
		this.host = host;
		this.port = port;
		this.endpoint = endpoint;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public URI getURI() throws URISyntaxException {
		return new URI("rtsp://" + host + ":" + port + endpoint);
	}
}
