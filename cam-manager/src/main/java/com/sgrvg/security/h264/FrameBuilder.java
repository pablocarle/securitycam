package com.sgrvg.security.h264;

import java.util.SortedSet;

import io.netty.buffer.ByteBuf;

public interface FrameBuilder {

	byte[] buildFrame(SortedSet<H264RtpPacket> packets);
	
	void buildFrame(SortedSet<H264RtpPacket> packets, ByteBuf destinationBuffer);
	
}
