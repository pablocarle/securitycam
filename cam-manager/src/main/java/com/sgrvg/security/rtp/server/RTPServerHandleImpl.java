package com.sgrvg.security.rtp.server;

import java.net.URISyntaxException;

import com.sgrvg.security.rtp.server.RTPServer.RTPServerTask;
import com.sgrvg.security.rtsp.RtspServerDefinition;

public class RTPServerHandleImpl implements RTPServerHandle {

	private RTPServerTask rtpTask;
	private RtspServerDefinition rtspServerDefinition;

	public RTPServerHandleImpl(RTPServerTask rtpTask, RtspServerDefinition server) {
		super();
		this.rtpTask = rtpTask;
		this.rtspServerDefinition = server;
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
}
