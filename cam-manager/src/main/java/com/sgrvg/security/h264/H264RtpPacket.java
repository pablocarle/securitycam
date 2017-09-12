package com.sgrvg.security.h264;

import java.util.Arrays;

import com.sgrvg.security.rtp.RtpPacket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

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
	
	private byte firstByte;
	private byte secondByte;
	
	public H264RtpPacket(final ByteBuf buffer, final ByteBufAllocator byteBufAllocator) throws IndexOutOfBoundsException {
		super(buffer, byteBufAllocator);
		decodeH264Fragment();
	}

	private void decodeH264Fragment() {
		firstByte = data.readByte();
		fragmentType = firstByte & 0x1F;
		secondByte = data.readByte();
		nalType = secondByte & 0x1F;
		startBit = secondByte & 0x80;
		endBit = secondByte & 0x40;
		data.readBytes(byteBufAllocator.buffer());
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
	
	/**
	 * Get processed data of this fragment's video data;
	 * 
	 * @return
	 */
	public byte[] getVideoData() {
		byte[] videoData;
		if (isStart()) {
			videoData = new byte[1 + otherVideoData.readableBytes()];
			byte part = (byte)(firstByte & 0xE0);
			byte combined = (byte)(part | (secondByte & 0x1F));
			videoData[0] = combined;
			otherVideoData.readBytes(videoData, 1, otherVideoData.readableBytes());
		} else {
			videoData = new byte[otherVideoData.readableBytes()];
			otherVideoData.readBytes(videoData);
		}
		return videoData;
	}
	
	public int getVideoDataSize() {
		if (isStart()) {
			return 1 + otherVideoData.readableBytes();
		} else {
			return otherVideoData.readableBytes();
		}
	}
	
	@Override
	public void release() {
		super.release();
		if (otherVideoData != null) {
			otherVideoData.release();
		}
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
	
	public byte getFirstByte() {
		return firstByte;
	}
}
