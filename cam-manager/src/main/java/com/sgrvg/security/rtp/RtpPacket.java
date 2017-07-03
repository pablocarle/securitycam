package com.sgrvg.security.rtp;

import java.util.ArrayList;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Takes from https://github.com/elasticsoftwarefoundation/elasterix/blob/master/rtp/src/main/java/org/elasticsoftware/rtp/packet/DataPacket.java
 * 
 * @author pabloc
 *
 */
public class RtpPacket {

	private RtpVersion version;
	private ArrayList<Long> contributingSourceIds;
	private boolean marker;
	private int payloadType;
	private int sequenceNumber;
	private long timestamp;
	private long ssrc;
	private short extensionHeaderData;
	private byte[] extensionData;
	private ByteBuf data;

	public static boolean isValidRTPPacket(ByteBuf buffer) {
		return buffer.readableBytes() >= 12;
	}
	
	public static RtpPacket decode(byte[] data) {
		return decode(Unpooled.wrappedBuffer(data));
	}
	
	public static RtpPacket decode(ByteBuf buffer) throws IndexOutOfBoundsException {
		if (!isValidRTPPacket(buffer)) {
			throw new IllegalArgumentException("A RTP packet must be at least 12 octets long");
		}

		// Version, Padding, eXtension, CSRC Count
		RtpPacket packet = new RtpPacket();
		byte b = buffer.readByte();
		packet.version = RtpVersion.fromByte(b);
		boolean padding = (b & 0x20) > 0; // mask 0010 0000
		boolean extension = (b & 0x10) > 0; // mask 0001 0000
		int contributingSourcesCount = b & 0x0f; // mask 0000 1111

		// Marker, Payload Type
		b = buffer.readByte();
		packet.marker = (b & 0x80) > 0; // mask 0000 0001
		packet.payloadType = (b & 0x7f); // mask 0111 1111

		packet.sequenceNumber = buffer.readUnsignedShort();
		packet.timestamp = buffer.readUnsignedInt();
		packet.ssrc = buffer.readUnsignedInt();

		// Read extension headers & data
		if (extension) {
			packet.extensionHeaderData = buffer.readShort();
			packet.extensionData = new byte[buffer.readUnsignedShort()];
			buffer.readBytes(packet.extensionData);
		}

		// Read CCRC's
		if (contributingSourcesCount > 0) {
			packet.contributingSourceIds = new ArrayList<>(contributingSourcesCount);
			for (int i = 0; i < contributingSourcesCount; i++) {
				long contributingSource = buffer.readUnsignedInt();
				packet.contributingSourceIds.add(contributingSource);
			}
		}

		if (!padding) {
			// No padding used, assume remaining data is the packet
			byte[] remainingBytes = new byte[buffer.readableBytes()];
			buffer.readBytes(remainingBytes);
			packet.setData(remainingBytes);
		} else {
			// Padding bit was set, so last byte contains the number of padding octets that should be discarded.
			short lastByte = buffer.getUnsignedByte(buffer.readerIndex() + buffer.readableBytes() - 1);
			byte[] dataBytes = new byte[buffer.readableBytes() - lastByte];
			buffer.readBytes(dataBytes);
			packet.setData(dataBytes);
			// Discard rest of buffer.
			buffer.skipBytes(buffer.readableBytes());
		}
		return packet;
	}
	
	@Override
	public String toString() {
		//TODO Deshabilitar para despliegue
		StringBuilder content = new StringBuilder();
		content.append("{\n\tversion: ");
		content.append(version);
		return content.toString();
	}

	private void setData(byte[] data) {
		this.data = Unpooled.wrappedBuffer(data);
	}
	
	private void setData(ByteBuf data) {
		this.data = data;
	}

	public RtpVersion getVersion() {
		return version;
	}

	public ArrayList<Long> getContributingSourceIds() {
		return contributingSourceIds;
	}

	public boolean isMarker() {
		return marker;
	}

	public int getPayloadType() {
		return payloadType;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public long getSsrc() {
		return ssrc;
	}

	public short getExtensionHeaderData() {
		return extensionHeaderData;
	}

	public byte[] getExtensionData() {
		return extensionData;
	}

	public ByteBuf getData() {
		return data;
	}
}
