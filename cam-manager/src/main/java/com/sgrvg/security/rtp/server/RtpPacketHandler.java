package com.sgrvg.security.rtp.server;

import static com.sgrvg.security.rtp.RtpPacket.isValidRTPPacket;

import java.util.List;

import com.sgrvg.security.rtp.RtpPacket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class RtpPacketHandler extends ByteToMessageDecoder {

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if (!isValidRTPPacket(in)) {
			return;
		}
		RtpPacket packet = RtpPacket.decode(in);
		out.add(packet);
	}
}
