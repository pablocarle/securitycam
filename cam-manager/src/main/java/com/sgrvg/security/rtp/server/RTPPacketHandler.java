package com.sgrvg.security.rtp.server;

import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.inject.Inject;
import com.sgrvg.security.ServerConfigHolder;
import com.sgrvg.security.SimpleLogger;
import com.sgrvg.security.VideoKeeper;
import com.sgrvg.security.h264.FrameBuilder;
import com.sgrvg.security.h264.H264RtpPacket;
import com.sgrvg.security.rtp.RtpPacket;
import com.sgrvg.security.rtsp.RtspServerDefinition;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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

	private static final int MAX_PACKET = 768 * 1024; // Tamaño de los bloques.

	private SimpleLogger logger;
	private FrameBuilder frameBuilder;
	private ServerConfigHolder serverConfig;

	private SortedSet<H264RtpPacket> packets = new TreeSet<>();

	private byte[] sps;
	private byte[] pps;

	private ByteBuf video;
	private boolean firstPacket = false;

	private long startTimestamp;
	private long endTimestamp;

	private long lastPacketReceived = -1L;
	
	private VideoKeeper driveVideoKeeper;
	private VideoKeeper localFileVideoKeeper;

	@Inject
	public RTPPacketHandler(SimpleLogger logger,
			FrameBuilder frameBuilder,
			ServerConfigHolder serverConfig,
			VideoKeeper driveVideoKeeper,
			VideoKeeper localFileVideoKeeper) {
		super();
		this.logger = logger;
		this.frameBuilder = frameBuilder;
		this.serverConfig = serverConfig;
		this.driveVideoKeeper = driveVideoKeeper;
		this.localFileVideoKeeper = localFileVideoKeeper;
		logger.info("Constructed RTPPacketHandler");
	}

	private void newH264Header() {
		video = Unpooled.wrappedBuffer(new byte[MAX_PACKET]);
		if (sps == null && pps == null) {
			Optional<RtspServerDefinition> rtspServer = serverConfig.getRtspEndpoint(this);
			if (rtspServer.isPresent()) {
				sps = rtspServer.get().getSessionDescription().getSps();
				pps = rtspServer.get().getSessionDescription().getPps();
			} else {
				throw new RuntimeException("Couldn't find bound rtsp endpoint");
			}
		}
		video.writeBytes(new byte[] {0x00,0x00,0x01});
		video.writeBytes(sps);
		video.writeBytes(new byte[] {0x00,0x00,0x01});
		video.writeBytes(pps);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
		ByteBuf content = msg.content();
		if (!RtpPacket.isValidRTPPacket(content)) {
			logger.info("INVALID RTP PACKET");
			return;
		}
		H264RtpPacket packet = new H264RtpPacket(content);
		doProcessPacket(packet);
		lastPacketReceived = System.currentTimeMillis();
	}

	private void doProcessPacket(H264RtpPacket packet) {
		if (packet.isStart()) {
			newH264Header();
			if (packets == null) {
				packets = new TreeSet<>();
			} else {
				packets.clear();
			}
			packets.add(packet);
		} else if (packet.isEnd()) {
			packets.add(packet);
			byte[] frame = frameBuilder.buildFrame(packets);
			if (video.writableBytes() >= frame.length) {
				video.writeBytes(frame);
				if (firstPacket) {
					firstPacket = false;
					startTimestamp = System.currentTimeMillis();
				}
			} else {
				firstPacket = true;
				endTimestamp = System.currentTimeMillis();
				ByteBuf videoBuffer = video.readBytes(video.readableBytes());
				doKeepVideo(startTimestamp, endTimestamp, videoBuffer);
				videoBuffer = null;
			}
		} else {
			packets.add(packet);
		}
	}

	private void doKeepVideo(long startTimestamp, long endTimestamp, ByteBuf videoBuffer) {
		Optional<RtspServerDefinition> definition = serverConfig.getRtspEndpoint(this);
		if (definition.isPresent()) {
			VideoKeeper keeper = null;
			switch (definition.get().getKeepType()) {
			case CLOUD_DRIVE:
				keeper = driveVideoKeeper;
				break;
			case LOCAL_FILE:
				keeper = localFileVideoKeeper;
				break;
			case CLOUD_DROPBOX:
			default:
				throw new RuntimeException("Unrecognized option " + definition.get().getKeepType().name());
			}
			keeper.keep(startTimestamp, endTimestamp, videoBuffer);
		} else {
			throw new IllegalStateException("Could not find rtsp server definition bound to this handler");
		}
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
