package com.sgrvg.security.rtsp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import com.google.common.base.Strings;

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
	private KeepType keepType;
	private int startHourSampling = 0;
	private int endHourSampling = 0;
	private String serverName;
	private Properties props;
	private int blockSize;
	private SessionDescription sessionDescription = new SessionDescription();

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
		String saveType = props.getProperty(serverName + "_save_type", "");
		switch (saveType.toLowerCase()) {
		case "local":
			this.keepType = KeepType.LOCAL_FILE;
			break;
		case "cloud_drive":
			this.keepType = KeepType.CLOUD_DRIVE;
			break;
		case "cloud_dropbox":
			this.keepType = KeepType.CLOUD_DROPBOX;
			break;
		default:
			throw new RTSPInitializationException("Unrecognized save type " + saveType);
		}
		String samplingHours = props.getProperty(serverName + "_sampling_hours", "");
		blockSize = Integer.parseInt(props.getProperty(serverName + "_block_size"));
		if (!Strings.isNullOrEmpty(samplingHours) && samplingHours.split("-").length == 2) {
			String[] hours = samplingHours.split("-");
			this.startHourSampling = Integer.parseInt(hours[0]);
			this.endHourSampling = Integer.parseInt(hours[1]);
		}
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

	public KeepType getKeepType() {
		return keepType;
	}

	public int getStartHourSampling() {
		return startHourSampling;
	}

	public int getEndHourSampling() {
		return endHourSampling;
	}
	
	public int getBlockSize() {
		return blockSize;
	}
	
	public String getServerName() {
		return serverName;
	}

	public SessionDescription getSessionDescription() {
		return sessionDescription;
	}

	public class SessionDescription {
		
		private byte[] sps;
		private byte[] pps;
		
		SessionDescription() {
			super();
		}

		public byte[] getSps() {
			return sps;
		}

		public void setSps(byte[] sps) {
			this.sps = sps;
		}

		public byte[] getPps() {
			return pps;
		}

		public void setPps(byte[] pps) {
			this.pps = pps;
		}
	}
}
