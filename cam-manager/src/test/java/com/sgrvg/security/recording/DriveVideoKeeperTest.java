package com.sgrvg.security.recording;

import com.sgrvg.security.SimpleLogger;
import io.netty.buffer.ByteBufAllocator;
import net.spy.memcached.MemcachedClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
public class DriveVideoKeeperTest {

	private final byte[] videoSource = null;
	
	private AbstractVideoKeeper keeper;
	
	@Mock
	private SimpleLogger logger;
	@Mock
	private ByteBufAllocator byteBufAllocator;
	@Mock
	private MemcachedClient memcachedClient;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		keeper = new DriveVideoKeeper(memcachedClient, logger, byteBufAllocator, 1000);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCompressVideo() {
		when(memcachedClient.get(any())).thenReturn(videoSource);
		when(memcachedClient.delete(any())).thenCallRealMethod();
		keeper.new VideoKeepTask("", true).run();
	}
}
