package com.sgrvg.security;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

public class CameraConnectionListener implements ChannelFutureListener {

	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
		System.out.println("Channel complete with result " + future.isSuccess());
	}

}
