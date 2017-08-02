package com.sgrvg.security.rtsp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
	private final List<KeepType> keepTypes = new ArrayList<>();
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
		String[] saveTypeTokens = saveType.split(",");
		Arrays.stream(saveTypeTokens)
		.filter(token -> !Strings.isNullOrEmpty(token))
		.forEach(token -> {
			switch (token.toLowerCase()) {
			case "local":
				keepTypes.add(KeepType.LOCAL_FILE);
				break;
			case "cloud_drive":
				keepTypes.add(KeepType.CLOUD_DRIVE);
				break;
			case "cloud_dropbox":
				keepTypes.add(KeepType.CLOUD_DROPBOX);
				break;
			default:
				throw new RTSPInitializationException("Unrecognized save type " + saveType);
			}
		});
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

	public List<KeepType> getKeepTypes() {
		return keepTypes;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((endpoint == null) ? 0 : endpoint.hashCode());
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + port;
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
		RtspServerDefinition other = (RtspServerDefinition) obj;
		if (endpoint == null) {
			if (other.endpoint != null)
				return false;
		} else if (!endpoint.equals(other.endpoint))
			return false;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (port != other.port)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "RtspServerDefinition [host=" + host + ", port=" + port + ", endpoint=" + endpoint + ", keepTypes="
				+ keepTypes + ", startHourSampling=" + startHourSampling + ", endHourSampling=" + endHourSampling
				+ ", serverName=" + serverName + ", props=" + props + ", blockSize=" + blockSize
				+ ", sessionDescription=" + sessionDescription + "]";
	}
}
