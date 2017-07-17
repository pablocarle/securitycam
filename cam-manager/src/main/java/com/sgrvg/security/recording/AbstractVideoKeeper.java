package com.sgrvg.security.recording;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import com.google.inject.Inject;
import com.sgrvg.security.SimpleLogger;
import com.sgrvg.security.VideoKeeper;

import io.netty.buffer.ByteBuf;
import net.spy.memcached.MemcachedClient;

/**
 * Implements concurrency logic
 * 
 * @author pabloc
 *
 */
public abstract class AbstractVideoKeeper implements VideoKeeper {

	protected static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd_hhmmss");
	
	protected static final String KEY_LAST_CLEANUP = "last_cleanup";
	
	protected SimpleLogger logger;
	
	private MemcachedClient memcachedClient;
	private ExecutorService executor;
	
	private volatile boolean lock = false;
	
	@Inject
	public AbstractVideoKeeper(MemcachedClient memcachedClient,
			SimpleLogger logger) {
		super();
		this.memcachedClient = memcachedClient;
		this.logger = logger;
		this.executor = Executors.newFixedThreadPool(5);
	}
	
	@Override
	public final void keep(long startTimestamp, long endTimestamp, String name, ByteBuf video) {
		video.resetReaderIndex();
		byte[] data = new byte[video.readableBytes()];
		video.readBytes(data);
		String startTime = SDF.format(new Date(startTimestamp));
		String endTime = SDF.format(new Date(endTimestamp));
		String key = name + "_" + startTime + "-" + endTime;
		memcachedClient.set(key, 3600 * 3, data);
		video = null;
		data = null;
		executor.submit(new VideoKeepTask(key));
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
			byte[] data = null;
			try {
				data = (byte[]) memcachedClient.get(key);
				memcachedClient.delete(key);
			} catch (Exception e) {
				logger.error("Failed getting key {} from memcached client", e, key);
			}
			if (data != null && data.length > 0) {
				Instant begin = Instant.now();
				data = compressVideo(data);
				doKeep(key, data);
				logger.info("Keeping of file with {} keeper took {} seconds", getID(), ChronoUnit.SECONDS.between(begin, Instant.now()));
			}
			data = null;
			if (!lock) {
				lock = true;
				Date lastCleanup = null;
				try {
					lastCleanup = (Date) memcachedClient.get(KEY_LAST_CLEANUP);
				} catch (Exception e) {
					logger.error("Failed getting last cleanup key from memcached client", e);
				}
				checkDateAndCleanup(lastCleanup);
				lock = false;
			}
		}

		private byte[] compressVideo(byte[] data) {
			FFmpegFrameGrabber frameGrabber = null;
			FFmpegFrameRecorder frameRecorder = null;
			
			try {
				InputStream is = new ByteArrayInputStream(data);
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length / 8);
				frameGrabber = new FFmpegFrameGrabber(is);
				frameGrabber.setFormat("h264");
				frameRecorder = new FFmpegFrameRecorder(outputStream, 0);
				
				frameGrabber.start();
				frameRecorder.setFormat("matroska");
				frameRecorder.setImageHeight(frameGrabber.getImageHeight());
				frameRecorder.setImageWidth(frameGrabber.getImageWidth());
				frameRecorder.setVideoCodecName("libx264");
				frameRecorder.start();
				Frame frame = null;
				while ((frame = frameGrabber.grab()) != null) {
					frameRecorder.record(frame);
				}
				byte[] outData = outputStream.toByteArray();
				logger.info("compressed {} bytes to {} bytes", data.length, outData.length);
				return outData;
			} catch (Exception e) {
				logger.error("Failed compressing video of size {} bytes. Fallback to raw h264", e, data.length);
				return data;
			} finally {
				if (frameGrabber != null) {
					try {
						frameGrabber.close();
					} catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
						logger.error("Failed closing frameGrabber resource", e);
					}
				}
				if (frameRecorder != null) {
					try {
						frameRecorder.close();
					} catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
						logger.error("Failed closing frameRecorder resource", e);
					}
				}
			}
		}

		private void checkDateAndCleanup(Date lastCleanup) {
			Instant begin = Instant.now();
			if (lastCleanup != null) {
				long days = ChronoUnit.DAYS.between(ZonedDateTime.ofInstant(lastCleanup.toInstant(), ZoneId.systemDefault()), 
						ZonedDateTime.now());
				if (days >= 1) {
					doCleanup(lastCleanup);
					memcachedClient.replace(KEY_LAST_CLEANUP, 3600 * 24 * 2, new Date());
					logger.info("Cleanup with {} keeper took {} seconds", getID(), ChronoUnit.SECONDS.between(begin, Instant.now()));
				}
			} else {
				doCleanup(lastCleanup);
				memcachedClient.set(KEY_LAST_CLEANUP, 3600 * 24 * 2, new Date());
				logger.info("Cleanup with {} keeper took {} seconds", getID(), ChronoUnit.SECONDS.between(begin, Instant.now()));
			}
		}
	}
}
