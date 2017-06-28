package com.sgrvg.security;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class CameraInboundHandler extends SimpleChannelInboundHandler<Object> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		System.out.println("Channel read " + this.getClass().getSimpleName());
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		System.out.println("Channel inactive " + this.getClass().getSimpleName());
	}

}
