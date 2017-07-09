package com.sgrvg.security.rtp.server;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.inject.Inject;
import com.sgrvg.security.ServerConfigHolder;
import com.sgrvg.security.SimpleLogger;
import com.sgrvg.security.h264.H264RtpPacket;
import com.sgrvg.security.rtp.RtpPacket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

public class RTPPacketHandler extends SimpleChannelInboundHandler<DatagramPacket> {

	private SimpleLogger logger;
	
	private SortedSet<H264RtpPacket> packets = new TreeSet<>();
	
	private byte[] sps = Base64.getDecoder().decode("Z2RAKawsqAoC/5U=");
	private byte[] pps = Base64.getDecoder().decode("aO44gA==");
	
	@Inject
	public RTPPacketHandler(SimpleLogger logger,
							ServerConfigHolder serverConfig) {
		super();
		this.logger = logger;
		logger.info("Constructed RTPPacketHandler");
		writeFileHeader();
	}
	
	private void writeFileHeader() {
		byte[] headers = new byte[6 + sps.length + pps.length];
		
		headers[0] = 0x00;
		headers[1] = 0x00;
		headers[2] = 0x01;
		int i = 3;
		for (int j = 0; i < headers.length && j < sps.length; i++, j++) {
			headers[i] = sps[j];
		}
		headers[i++] = 0x00;
		headers[i++] = 0x00;
		headers[i++] = 0x01;
		for (int j = 0; i < headers.length && j < pps.length; i++, j++) {
			headers[i] = pps[j];
		}
		
		try {
			Files.write(Paths.get(new URI("file:///home/pabloc/test.264")), headers, StandardOpenOption.CREATE);
		} catch (IOException | URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
		ByteBuf content = msg.content();
		if (!RtpPacket.isValidRTPPacket(content)) {
			logger.info("INVALID RTP PACKET");
			return;
		}
		H264RtpPacket packet = new H264RtpPacket(content);
		
		if (packet.isStart()) {
			if (packets == null) {
				packets = new TreeSet<>();
			} else {
				packets.clear();
			}
			packets = new TreeSet<>();
			packets.add(packet);
		} else if (packet.isEnd()) {
			try {
				packets.add(packet);
				byte[] frame = buildFrame();
				logger.info("Write frame of length: {}", frame.length);
				Files.write(Paths.get(new URI("file:///home/pabloc/test.264")), frame, StandardOpenOption.APPEND);
			} catch (Exception e) {
				logger.error("Failed building", e);
			}
		} else {
			packets.add(packet);
		}
	}

	private byte[] buildFrame() {
		byte[] frame = new byte[3 + packets.stream().mapToInt(packet -> packet.getVideoDataSize()).sum()];
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
		return frame;
	}
}
