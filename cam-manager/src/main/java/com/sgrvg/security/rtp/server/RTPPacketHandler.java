package com.sgrvg.security.rtp.server;

import java.util.Base64;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.inject.Inject;
import com.sgrvg.security.ServerConfigHolder;
import com.sgrvg.security.SimpleLogger;
import com.sgrvg.security.VideoKeeper;
import com.sgrvg.security.h264.FrameBuilder;
import com.sgrvg.security.h264.H264RtpPacket;
import com.sgrvg.security.rtp.RtpPacket;

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

	private byte[] sps = Base64.getDecoder().decode("Z2RAKawsqAoC/5U="); //TODO Esto debe obtenerse de la configuration del server RTSP
	private byte[] pps = Base64.getDecoder().decode("aO44gA==");

	private ByteBuf video;
	private boolean firstPacket = false;
	
	private long startTimestamp;
	private long endTimestamp;

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
	}

	private void doProcessPacket(H264RtpPacket packet) {
		if (packet.isStart()) {
			if (packets == null) {
				packets = new TreeSet<>();
			} else {
				packets.clear();
			}
			packets = new TreeSet<>();
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
				newH264Header();
				firstPacket = true;
				endTimestamp = System.currentTimeMillis();
				ByteBuf videoBuffer = video.readBytes(video.readableBytes());
				video = null;
				doKeepVideo(startTimestamp, endTimestamp, videoBuffer);
				videoBuffer = null;
			}
		} else {
			packets.add(packet);
		}
	}

	private void doKeepVideo(long startTimestamp2, long endTimestamp2, ByteBuf videoBuffer) {
		videoKeeper.keep(startTimestamp, endTimestamp, videoBuffer);
	}
}
