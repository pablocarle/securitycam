package com.sgrvg.security.rtsp.client;

import com.sgrvg.security.rtsp.client.RtspClient.RtspClientTask;

public class RtspClientHandleImpl implements RtspClientHandle {

	private RtspClientTask rtspTask;

	public RtspClientHandleImpl(RtspClientTask rtspTask) {
		super();
		this.rtspTask = rtspTask;
	}

	@Override
	public void teardown() {

	}

}
