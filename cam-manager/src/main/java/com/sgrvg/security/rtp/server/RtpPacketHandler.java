package com.sgrvg.security.rtp.server;

import static com.sgrvg.security.rtp.RtpPacket.isValidRTPPacket;

import java.util.List;

import com.google.inject.Inject;
import com.sgrvg.security.SimpleLogger;
import com.sgrvg.security.rtp.RtpPacket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class RtpPacketHandler extends ByteToMessageDecoder {

	private final SimpleLogger logger;

	@Inject
	public RtpPacketHandler(SimpleLogger logger) {
		super();
		this.logger = logger;
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if (!isValidRTPPacket(in)) {
			return;
		}
		RtpPacket packet = RtpPacket.decode(in);
		logger.info("Received packet {}", packet);
		out.add(packet);
	}
}
