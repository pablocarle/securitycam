package com.sgrvg.security.rtp.server;

import com.google.inject.Inject;
import com.sgrvg.security.SimpleLogger;
import com.sgrvg.security.rtp.RtpPacket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

public class RTPPacketHandler extends SimpleChannelInboundHandler<DatagramPacket> {

	private SimpleLogger logger;

	//TODO Memcachedclient?
	@Inject
	public RTPPacketHandler(SimpleLogger logger) {
		super();
		this.logger = logger;
		logger.info("Constructed RTPPacketHandler");
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
		logger.info("channel read UDP");
		ByteBuf content = msg.content();
		if (!RtpPacket.isValidRTPPacket(content)) {
			logger.info("INVALID RTP PACKET");
			return;
		}
		RtpPacket packet = RtpPacket.decode(content);
		//TODO Que hago con el packet?
	}

}
