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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + port;
		result = prime * result + ((rtspServerUri == null) ? 0 : rtspServerUri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RTPServerDefinition other = (RTPServerDefinition) obj;
		if (port != other.port)
			return false;
		if (rtspServerUri == null) {
			if (other.rtspServerUri != null)
				return false;
		} else if (!rtspServerUri.equals(other.rtspServerUri))
			return false;
		return true;
	}
}
