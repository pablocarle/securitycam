package com.sgrvg.security.rtp.server;

import com.sgrvg.security.rtp.server.RTPServer.RTPServerTask;

public class RTPServerHandleImpl implements RTPServerHandle {

	private RTPServerTask rtpTask;

	public RTPServerHandleImpl(RTPServerTask rtpTask) {
		super();
		this.rtpTask = rtpTask;
	}

	@Override
	public RTPServerDefinition serverDefinition() {
		return new RTPServerDefinition(rtpTask.getAssignedPort());
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
}
