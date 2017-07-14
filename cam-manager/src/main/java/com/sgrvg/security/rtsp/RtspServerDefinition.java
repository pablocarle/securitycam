package com.sgrvg.security.rtsp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * Holds definition of a RTSP Server stream
 * 
 * @author pabloc
 *
 */
public class RtspServerDefinition {

	public enum KeepType {
		CLOUD_DRIVE, CLOUD_DROPBOX, LOCAL_FILE;
	}

	private String host;
	private int port;
	private String endpoint;
	private String name;
	private KeepType keepType;
	private int startHourSampling;
	private int endHourSampling;
	private String serverName;
	private Properties props;
	private SessionDescription sessionDescription;

	public RtspServerDefinition(String serverName, Properties props) {
		super();
		this.serverName = serverName;
		this.props = props;
		loadProps();
	}

	private void loadProps() {
		host = props.getProperty(serverName + "_ip");
		port = Integer.valueOf(props.getProperty(serverName + "_port"));
		endpoint = props.getProperty(serverName + "_path");
		//TODO Falta parsing
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

	public String getName() {
		return name;
	}

	public URI getURI() throws URISyntaxException {
		return new URI("rtsp://" + host + ":" + port + endpoint);
	}

	public KeepType getKeepType() {
		return keepType;
	}

	public int getStartHourSampling() {
		return startHourSampling;
	}

	public int getEndHourSampling() {
		return endHourSampling;
	}
	
	public class SessionDescription {
		
		private byte[] sps;
		private byte[] pps;
		
	}
}
