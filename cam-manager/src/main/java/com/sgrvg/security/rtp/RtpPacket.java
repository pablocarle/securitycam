package com.sgrvg.security.rtp;

import java.util.ArrayList;
import java.util.Arrays;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;

/**
 * Takes from https://github.com/elasticsoftwarefoundation/elasterix/blob/master/rtp/src/main/java/org/elasticsoftware/rtp/packet/DataPacket.java
 * 
 * @author pabloc
 *
 */
public class RtpPacket implements Comparable<RtpPacket> {

	protected final ByteBufAllocator byteBufAllocator;
	
	protected RtpVersion version;
	protected ArrayList<Long> contributingSourceIds;
	protected boolean marker;
	protected int payloadType;
	protected int sequenceNumber;
	protected long timestamp;
	protected long ssrc;
	protected short extensionHeaderData;
	protected byte[] extensionData;
	protected ByteBuf data;

	public static boolean isValidRTPPacket(ByteBuf buffer) {
		return buffer.readableBytes() >= 12;
	}
	
	public RtpPacket(final ByteBuf buffer, final ByteBufAllocator byteBufAllocator) throws IndexOutOfBoundsException {
		super();
		this.byteBufAllocator = byteBufAllocator;
		if (!isValidRTPPacket(buffer)) {
			throw new IllegalArgumentException("A RTP packet must be at least 12 octets long");
		}

		// Version, Padding, eXtension, CSRC Count
		byte b = buffer.readByte();
		this.version = RtpVersion.fromByte(b);
		boolean padding = (b & 0x20) > 0; // mask 0010 0000
		boolean extension = (b & 0x10) > 0; // mask 0001 0000
		int contributingSourcesCount = b & 0x0f; // mask 0000 1111

		// Marker, Payload Type
		b = buffer.readByte();
		this.marker = (b & 0x80) > 0; // mask 0000 0001
		this.payloadType = (b & 0x7f); // mask 0111 1111

		this.sequenceNumber = buffer.readUnsignedShort();
		this.timestamp = buffer.readUnsignedInt();
		this.ssrc = buffer.readUnsignedInt();

		// Read extension headers & data
		if (extension) {
			this.extensionHeaderData = buffer.readShort();
			this.extensionData = new byte[buffer.readUnsignedShort()];
			buffer.readBytes(this.extensionData);
		}

		// Read CCRC's
		if (contributingSourcesCount > 0) {
			this.contributingSourceIds = new ArrayList<>(contributingSourcesCount);
			for (int i = 0; i < contributingSourcesCount; i++) {
				long contributingSource = buffer.readUnsignedInt();
				this.contributingSourceIds.add(contributingSource);
			}
		}

		if (!padding) {
			// No padding used, assume remaining data is the packet
			this.setData(buffer.readBytes(byteBufAllocator.buffer()));
		} else {
			// Padding bit was set, so last byte contains the number of padding octets that should be discarded.
			short lastByte = buffer.getUnsignedByte(buffer.readerIndex() + buffer.readableBytes() - 1);
			this.setData(buffer.readBytes(byteBufAllocator.buffer(), buffer.readableBytes() - lastByte));
			// Discard rest of buffer.
			buffer.skipBytes(buffer.readableBytes());
		}
	}
	
	@Override
	public String toString() {
		return "RtpPacket [version=" + version + ", marker=" + marker + ", payloadType=" + payloadType
				+ ", sequenceNumber=" + sequenceNumber + ", timestamp=" + timestamp + ", ssrc=" + ssrc
				+ ", extensionHeaderData=" + extensionHeaderData + ", extensionData=" + Arrays.toString(extensionData)
				+ ", data=" + data + "]";
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
	
	public void release() {
		if (contributingSourceIds != null && !contributingSourceIds.isEmpty()) {
			contributingSourceIds.clear();
		}
		if (data != null) {
			ReferenceCountUtil.release(data);
		}
		extensionData = null;
	}
	
	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (marker ? 1231 : 1237);
		result = prime * result + payloadType;
		result = prime * result + sequenceNumber;
		result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RtpPacket other = (RtpPacket) obj;
		if (marker != other.marker)
			return false;
		if (payloadType != other.payloadType)
			return false;
		if (sequenceNumber != other.sequenceNumber)
			return false;
		if (timestamp != other.timestamp)
			return false;
		return true;
	}

	@Override
	public int compareTo(RtpPacket o) {
		if (this.equals(o)) {
			return 0;
		}
		int compare = Integer.compare(this.sequenceNumber, o.sequenceNumber);
		if (compare == 0) {
			compare = Long.compare(this.timestamp, o.timestamp);
		}
		return compare;
	}
}
