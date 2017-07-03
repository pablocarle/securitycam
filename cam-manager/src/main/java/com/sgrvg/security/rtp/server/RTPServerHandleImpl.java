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
	public void waitConnected() throws InterruptedException {
		if (rtpTask.isSuccessfulConnection()) {
			return;
		} else {
			while (!rtpTask.isSuccessfulConnection()) {
				Thread.sleep(1000);
			}
		}
	}
}
