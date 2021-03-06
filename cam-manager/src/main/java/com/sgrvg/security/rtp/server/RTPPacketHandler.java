package com.sgrvg.security.rtp.server;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sgrvg.security.ServerConfigHolder;
import com.sgrvg.security.SimpleLogger;
import com.sgrvg.security.VideoKeeper;
import com.sgrvg.security.h264.FrameBuilder;
import com.sgrvg.security.h264.H264RtpPacket;
import com.sgrvg.security.rtp.RtpPacket;
import com.sgrvg.security.rtsp.RtspServerDefinition;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

/**
 * Collector of RTP Packets, re-builds h264 frames and sends to persistance
 * 
 * @author pabloc
 *
 */
public class RTPPacketHandler extends SimpleChannelInboundHandler<DatagramPacket> {

	private static final byte[] frameHeader = new byte[] {0x00,0x00,0x01};

	private final SimpleLogger logger;
	private final FrameBuilder frameBuilder;
	private final ServerConfigHolder serverConfig;

	private final VideoKeeper driveVideoKeeper;
	private final VideoKeeper localFileVideoKeeper;
	private final VideoKeeper dropboxVideoKeeper;
	
	private final ByteBufAllocator byteBufAllocator;

	private SortedSet<H264RtpPacket> packets = new TreeSet<>();

	private byte[] sps;
	private byte[] pps;

	private int blockSize = 0;

	private ByteBuf video;
	private boolean firstPacket = false;

	private long startTimestamp = System.currentTimeMillis();
	private long endTimestamp;

	private volatile long lastPacketReceived = -1L;


	private int maxCapacity;

	@Inject
	public RTPPacketHandler(SimpleLogger logger,
			FrameBuilder frameBuilder,
			ServerConfigHolder serverConfig,
			@Named("drive_keeper") VideoKeeper driveVideoKeeper,
			@Named("file_keeper") VideoKeeper localFileVideoKeeper,
			@Named("dropbox_keeper") VideoKeeper dropboxVideoKeeper,
			ByteBufAllocator byteBufAllocator) {
		super();
		this.logger = logger;
		this.frameBuilder = frameBuilder;
		this.serverConfig = serverConfig;
		this.driveVideoKeeper = driveVideoKeeper;
		this.localFileVideoKeeper = localFileVideoKeeper;
		this.dropboxVideoKeeper = dropboxVideoKeeper;
		this.byteBufAllocator = byteBufAllocator;
		logger.info("Constructed RTPPacketHandler");
	}

	private void newH264Header() {
		if (blockSize == 0) {
			Optional<RtspServerDefinition> rtspServer = serverConfig.getRtspEndpoint(this);
			if (rtspServer.isPresent()) {
				blockSize = rtspServer.get().getBlockSize();
			} else {
				throw new RuntimeException("Couldn't find bound rtsp endpoint");
			}
		}
		maxCapacity = blockSize * 1024 * 1024;
		video = byteBufAllocator.buffer(blockSize * 1024, maxCapacity);
		video.resetWriterIndex();
		video.resetReaderIndex();
		if (sps == null || sps.length == 0) {
			loadSps();
		}
		if (pps == null || pps.length == 0) {
			loadPps();
		}
		video.writeBytes(frameHeader);
		video.writeBytes(sps);
		video.writeBytes(frameHeader);
		video.writeBytes(pps);
	}

	private void loadPps() {
		Optional<RtspServerDefinition> rtspServer = serverConfig.getRtspEndpoint(this);
		if (rtspServer.isPresent()) {
			pps = rtspServer.get().getSessionDescription().getPps();
		} else {
			throw new RuntimeException("Couldn't find bound rtsp endpoint");
		}
	}

	private void loadSps() {
		Optional<RtspServerDefinition> rtspServer = serverConfig.getRtspEndpoint(this);
		if (rtspServer.isPresent()) {
			sps = rtspServer.get().getSessionDescription().getSps();
		} else {
			throw new RuntimeException("Couldn't find bound rtsp endpoint");
		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
		channelRead0(msg.content());
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error("Exception caught in RTPPacketHandler", cause);
		super.exceptionCaught(ctx, cause);
	}
	
	@VisibleForTesting
	protected void channelRead0(ByteBuf content) {
		if (!RtpPacket.isValidRTPPacket(content)) {
			logger.info("INVALID RTP PACKET");
			return;
		}
		H264RtpPacket packet = new H264RtpPacket(content, byteBufAllocator);
		doProcessPacket(packet);
		lastPacketReceived = System.currentTimeMillis();
	}

	private void doProcessPacket(H264RtpPacket packet) {
		if (packet.isStart()) {
			if (video == null) {
				newH264Header();
			}
			if (packets == null) {
				packets = new TreeSet<>();
			} else {
				clearPackets();
			}
			packets.add(packet);
		} else if (packet.isEnd()) {
			packets.add(packet);
			ByteBuf frame = byteBufAllocator.buffer();
			frameBuilder.buildFrame(packets, frame);
			if ((maxCapacity - video.readableBytes()) >= frame.readableBytes()) {
				video.writeBytes(frame);
				frame.release();
				if (firstPacket) {
					firstPacket = false;
					startTimestamp = System.currentTimeMillis();
				}
			} else {
				firstPacket = true;
				endTimestamp = System.currentTimeMillis();
				doKeepVideo(startTimestamp, endTimestamp, video);
				video.release();
				video = null;
				newH264Header();
				//Asumo que siempre va a entrar al menos 1 frame
				video.writeBytes(frame);
				frame.release();
			}
		} else {
			packets.add(packet);
		}
	}

	private void clearPackets() {
		packets.forEach(RtpPacket::release);
		packets.clear();
	}

	private void doKeepVideo(final long startTimestamp, final long endTimestamp, final ByteBuf videoBuffer) {
		Optional<RtspServerDefinition> definition = serverConfig.getRtspEndpoint(this);
		if (definition.isPresent()) {
			VideoKeeper keeper;
			switch (definition.get().getKeepType()) {
			case CLOUD_DRIVE:
				keeper = driveVideoKeeper;
				break;
			case LOCAL_FILE:
				keeper = localFileVideoKeeper;
				break;
			case CLOUD_DROPBOX:
				keeper = dropboxVideoKeeper;
				break;
			default:
				throw new RuntimeException("Unrecognized option " + definition.get().getKeepType().name());
			}
			keeper.keep(startTimestamp, endTimestamp, definition.get().getServerName(), videoBuffer, definition.get().doCompression());
		} else {
			throw new IllegalStateException("Could not find rtsp server definition bound to this handler");
		}
	}

	public Optional<Instant> getLastTimePacketReceived() {
		if (lastPacketReceived <= 0) {
			return Optional.empty();
		} else {
			return Optional.of(Instant.ofEpochMilli(lastPacketReceived));
		}
	}

	long getMsSinceLastPacket() {
		return getLastTimePacketReceived().map(value -> Math.abs(ChronoUnit.MILLIS.between(value, Instant.now()))).orElse(-1L);
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	void restart() {
		packets = new TreeSet<>();
		sps = null;
		pps = null;
		if (video != null) {
			doKeepVideo(startTimestamp, endTimestamp, video);
			video.release();
			video = null;
		}
	}
}
