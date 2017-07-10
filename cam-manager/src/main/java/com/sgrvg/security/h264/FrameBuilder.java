package com.sgrvg.security.h264;

import java.util.SortedSet;

public interface FrameBuilder {

	byte[] buildFrame(SortedSet<H264RtpPacket> packets);
	
}
