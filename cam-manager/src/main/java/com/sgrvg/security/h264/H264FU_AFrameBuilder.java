package com.sgrvg.security.h264;

import java.util.Iterator;
import java.util.SortedSet;

import com.google.inject.Inject;

import io.netty.buffer.ByteBuf;

/**
 * Frame builder using de-packetized packets
 * 
 * @author pabloc
 *
 */
public class H264FU_AFrameBuilder implements FrameBuilder {

	@Inject	
	public H264FU_AFrameBuilder() {
		super();
	}
	
	@Override
	public byte[] buildFrame(SortedSet<H264RtpPacket> packets) {
		final byte[] frame = new byte[3 + packets.stream().mapToInt(packet -> packet.getVideoDataSize()).sum()];
		frame[0] = 0x00;
		frame[1] = 0x00;
		frame[2] = 0x01;

		int i = 3;
		int j = 0;
		
		int videoSize = 0;
		byte[] fragment = null;
		
		H264RtpPacket current = null;
		Iterator<H264RtpPacket> it = packets.iterator();
		while (it.hasNext()) {
			current = it.next();
			fragment = current.getVideoData();
			videoSize = fragment.length;
			for (j = 0; i < frame.length && j < videoSize; i++, j++) {
				frame[i] = fragment[j];
			}
		}
		it = null;
		return frame;
	}

	@Override
	public void buildFrame(SortedSet<H264RtpPacket> packets, ByteBuf destinationBuffer) {
		destinationBuffer.resetWriterIndex();
		destinationBuffer.writeBytes(new byte[] {0x00,0x00,0x01});
		packets.forEach(packet -> {
			destinationBuffer.writeBytes(packet.getVideoData());
		});
	}
}
