package com.sgrvg.security;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
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
	
	protected static final String KEY_LAST_CLEANUP = "last_cleanup";
	
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
			video = null;
			executor.submit(new VideoKeepTask(key));
		} else {
			throw new RuntimeException("Expected video to be backed by a byte array");
		}
	}
	
	protected abstract void doKeep(String key, byte[] data);
	
	protected abstract void doCleanup(Date lastCleanup);

	/**
	 * Runnable to do keeping and cleanup task
	 * 
	 * @author pabloc
	 *
	 */
	private class VideoKeepTask implements Runnable {

		private String key;

		public VideoKeepTask(String key) {
			super();
			this.key = key;
		}
		
		@Override
		public void run() {
			byte[] data = (byte[]) memcachedClient.get(key);
			memcachedClient.delete(key);
			if (data != null && data.length > 0) {
				doKeep(key, data);
			}
			data = null;
			Date lastCleanup = (Date) memcachedClient.get(KEY_LAST_CLEANUP);
			if (lastCleanup != null) {
				long days = ChronoUnit.DAYS.between(ZonedDateTime.ofInstant(lastCleanup.toInstant(), ZoneId.systemDefault()), 
						ZonedDateTime.now());
				if (days >= 1) {
					doCleanup(lastCleanup);
					memcachedClient.replace(KEY_LAST_CLEANUP, 3600 * 24 * 2, new Date());
				}
			} else {
				doCleanup(lastCleanup);
				memcachedClient.set(KEY_LAST_CLEANUP, 3600 * 24 * 2, new Date());
			}
		}
	}
}