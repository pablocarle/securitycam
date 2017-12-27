package com.sgrvg.security.recording;

import com.sgrvg.security.SimpleLogger;
import io.netty.buffer.ByteBufAllocator;
import net.spy.memcached.MemcachedClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.MockitoAnnotations.initMocks;

public class LocalFileVideoKeeperTest {

    private LocalFileVideoKeeper keeper;

    @Mock
    private MemcachedClient memcachedClient;
    @Mock
    private SimpleLogger logger;
    @Mock
    private ByteBufAllocator allocator;

    @Before
    public void setUp() {
        this.keeper = new LocalFileVideoKeeper(memcachedClient, logger, allocator, 1000);
        initMocks(this);
    }

    @Test
    public void testIOException() {

    }

}
