package com.sgrvg.security.rtp.server;

import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.google.common.base.Strings;
import com.sgrvg.security.rtp.server.RTPServer.RTPServerTask;
import com.sgrvg.security.rtsp.RtspServerDefinition;

public final class RTPServerHandleImpl implements RTPServerHandle {

	private RTPServerTask rtpTask;
	private RtspServerDefinition rtspServerDefinition;
	private String id;

	RTPServerHandleImpl(String id, RTPServerTask rtpTask, RtspServerDefinition server) {
		super();
		if (Strings.isNullOrEmpty(id)) {
			throw new NullPointerException("ID cannot be null nor empty");
		}
		this.rtpTask = rtpTask;
		this.rtspServerDefinition = server;
		this.id = id;
	}

	@Override
	public RTPServerDefinition serverDefinition() throws URISyntaxException {
		return new RTPServerDefinition(rtpTask.getAssignedPort(), rtspServerDefinition.getURI());
	}

	@Override
	public void waitConnected() throws InterruptedException, RTPServerInitializationException {
		if (rtpTask.isSuccessfulConnection()) {
			return;
		} else if (rtpTask.isFailedConnection()) {
			throw new RTPServerInitializationException("Failed to connect RTP server");
		} else {
			while (!rtpTask.isSuccessfulConnection() || rtpTask.isFailedConnection()) {
				Thread.sleep(1000);
			}
			if (rtpTask.isFailedConnection()) {
				throw new RTPServerInitializationException("Failed to connect RTP server");
			}
		}
	}

	@Override
	public String getID() {
		return id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		RTPServerHandleImpl other = (RTPServerHandleImpl) obj;
		if (id == null) {
			return other.id == null;
		} else return id.equals(other.id);
	}

	@Override
	public boolean receiving() {
		return !rtpTask.isFailedConnection() && rtpTask.isSuccessfulConnection()
				&& !rtpTask.isShutdown();
	}

	@Override
	public void shutdown() {
		rtpTask.shutdown();
	}

	@Override
	public long getTimeSinceLastPacket(ChronoUnit chronoUnit) {
		return rtpTask.getLastReceivedPacket().map(value -> Math.abs(chronoUnit.between(value, Instant.now())))
				.orElse(-1L);
	}
}
