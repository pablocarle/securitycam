package com.sgrvg.security.rtp.server;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sgrvg.security.ServerConfigHolder;
import com.sgrvg.security.SimpleLogger;
import com.sgrvg.security.VideoKeeper;
import com.sgrvg.security.h264.FrameBuilder;
import com.sgrvg.security.rtp.server.RTPPacketHandler;
import com.sgrvg.security.rtsp.RtspServerDefinition;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

public class RTPPacketHandlerTest {

	private RTPPacketHandler packetHandler;

	private byte[] data;

	@Mock
	private SimpleLogger logger;
	@Mock
	private VideoKeeper keeper;
	@Mock
	private ServerConfigHolder serverConfig;
	@Mock
	private FrameBuilder frameBuilder;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		ByteBufAllocator byteBufAllocator = ByteBufAllocator.DEFAULT;
		this.packetHandler = new RTPPacketHandler(logger, frameBuilder, serverConfig, keeper, keeper, keeper, byteBufAllocator);
		mockCommon();
		try {
			data = Files.readAllBytes(Paths.get(new URI("file:///home/pabloc/data3.out")));
		} catch (IOException | URISyntaxException e) {
			data = new byte[0];
		}
	}

	private void mockCommon() {
		Optional<RtspServerDefinition> optional = null;
		when(serverConfig.getRtspEndpoint((RTPPacketHandler)any()))
			.thenReturn(optional);
		when(serverConfig.getRtspEndpoint((RTPServerDefinition)any()))
			.thenReturn(optional);
	}

	@Test
	public void testHandler() {
		try {
			packetHandler.channelRead0(getNextRtpPacket());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private ByteBuf getNextRtpPacket() {
		return Unpooled.wrappedBuffer(data);
	}
}
