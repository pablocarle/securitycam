package com.sgrvg.security;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.inject.Inject;

import io.netty.buffer.ByteBuf;
import net.spy.memcached.MemcachedClient;

/**
 * @author pabloc
 *
 */
public abstract class AbstractVideoKeeper implements VideoKeeper {

	protected static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd_hhmmss");
	
	protected SimpleLogger logger;
	
	private MemcachedClient memcachedClient;
	private ExecutorService executor;
	
	@Inject
	public AbstractVideoKeeper(MemcachedClient memcachedClient,
			SimpleLogger logger) {
		super();
		this.memcachedClient = memcachedClient;
		this.logger = logger;
		this.executor = Executors.newFixedThreadPool(5);
	}
	
	@Override
	public final void keep(long startTimestamp, long endTimestamp, ByteBuf video) {
		if (video.hasArray()) {
			String startTime = SDF.format(new Date(startTimestamp));
			String endTime = SDF.format(new Date(endTimestamp));
			String key = startTime + "-" + endTime;
			memcachedClient.set(key, 3600 * 3, video.array());
			executor.submit(new VideoKeepTask(key));
		} else {
			throw new RuntimeException("Expected video to be backed by a byte array");
		}
	}
	
	protected abstract void doKeep(String key, byte[] data);

	private class VideoKeepTask implements Runnable {

		private String key;

		public VideoKeepTask(String key) {
			super();
			this.key = key;
		}
		
		@Override
		public void run() {
			byte[] data = (byte[]) memcachedClient.get(key);
			if (data != null && data.length > 0) {
				doKeep(key, data);
			}
		}
	}
}
