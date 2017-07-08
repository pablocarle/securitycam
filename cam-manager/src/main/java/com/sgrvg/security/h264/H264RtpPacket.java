package com.sgrvg.security.h264;

import java.util.Arrays;

import com.sgrvg.security.rtp.RtpPacket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This assumes it is packet type 28, FU-A
 * 
 * @author pabloc
 *
 */
public class H264RtpPacket extends RtpPacket {

	private ByteBuf otherVideoData;
	private int fragmentType;
	private int nalType;
	private int startBit = 0;
	private int endBit = 0;
	
	public H264RtpPacket(ByteBuf buffer) throws IndexOutOfBoundsException {
		super(buffer);
		decodeH264Fragment();
	}

	private void decodeH264Fragment() {
		byte firstByte = data.readByte();
		fragmentType = firstByte & 0x1F;
		byte secondByte = data.readByte();
		nalType = secondByte & 0x1F;
		startBit = secondByte & 0x80;
		endBit = secondByte & 0x40;
		byte[] remainingBytes = new byte[data.readableBytes()];
		data.readBytes(remainingBytes);
		otherVideoData = Unpooled.wrappedBuffer(remainingBytes);
	}

	@Override
	public String toString() {
		return "H264RtpPacket [otherVideoData=" + otherVideoData + ", fragmentType=" + fragmentType + ", nalType="
				+ nalType + ", startBit=" + startBit + ", endBit=" + endBit + ", version=" + version
				+ ", contributingSourceIds=" + contributingSourceIds + ", marker=" + marker + ", payloadType="
				+ payloadType + ", sequenceNumber=" + sequenceNumber + ", timestamp=" + timestamp + ", ssrc=" + ssrc
				+ ", extensionHeaderData=" + extensionHeaderData + ", extensionData=" + Arrays.toString(extensionData)
				+ ", data=" + data + "]";
	}
	
	public boolean isStart() {
		return startBit != 0;
	}
	
	public boolean isEnd() {
		return endBit != 0;
	}
	
	public boolean isMiddle() {
		return !isStart() && !isEnd();
	}

	public ByteBuf getOtherVideoData() {
		return otherVideoData;
	}

	public int getFragmentType() {
		return fragmentType;
	}

	public int getNalType() {
		return nalType;
	}

	public int getStartBit() {
		return startBit;
	}

	public int getEndBit() {
		return endBit;
	}
}
