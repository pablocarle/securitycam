package com.sgrvg.security.recording;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.sgrvg.security.SimpleLogger;
import com.sgrvg.security.VideoKeeper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import net.spy.memcached.MemcachedClient;

/**
 * Implements concurrency logic
 * 
 * @author pabloc
 *
 */
public abstract class AbstractVideoKeeper implements VideoKeeper {

	protected static final String KEY_LAST_CLEANUP = "last_cleanup";
	
	protected SimpleLogger logger;
	
	private MemcachedClient memcachedClient;
	private ExecutorService executor;
	private ScheduledExecutorService timeoutService;

	private Map<Runnable, ScheduledFuture<?>> executorTimeoutMap;
	
	private ByteBufAllocator byteBufAllocator;
	private int videoBitrate;
	
	private volatile boolean lock = false;

	@Inject
	public AbstractVideoKeeper(
			MemcachedClient memcachedClient,
			SimpleLogger logger,
			ByteBufAllocator bytebufAllocator,
			int videoBitrate) {
		super();
		this.memcachedClient = memcachedClient;
		this.logger = logger;
		this.executor = Executors.newFixedThreadPool(3, new ThreadFactory() {
			final ThreadFactory delegate = Executors.defaultThreadFactory();
			
			@Override
			public Thread newThread(Runnable r) {
				final Thread result = delegate.newThread(r);
				result.setName(getID() + result.getName());
				result.setDaemon(true);
				return result;
			}
		});
		this.timeoutService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			final ThreadFactory delegate = Executors.defaultThreadFactory();
			
			@Override
			public Thread newThread(Runnable r) {
				final Thread result = delegate.newThread(r);
				result.setName("TimeoutService-" + getID() + "-" + result.getName());
				result.setDaemon(true);
				return result;
			}
		});
		this.executorTimeoutMap = new ConcurrentHashMap<>();
		this.byteBufAllocator = bytebufAllocator;
		this.videoBitrate = videoBitrate;
	}
	
	@Override
	public final void keep(long startTimestamp, long endTimestamp, String name, ByteBuf video, boolean doCompression) {
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_hhmmss");
		video.resetReaderIndex();
		byte[] data = new byte[video.readableBytes()];
		video.readBytes(data);
		String startTime = sdf.format(new Date(startTimestamp));
		String endTime = sdf.format(new Date(endTimestamp));
		String key = name + "_" + startTime + "-" + endTime;
		try {
			memcachedClient.set(key, 3600 * 3, data);
			video = null;
			data = null;
			submitTask(key, startTimestamp, endTimestamp, doCompression);
			logger.info("Submitted task with keeper {} and key {}", getID(), key);
		} catch (Exception e) {
			logger.error("Failed to keep video. {} bytes data lost", e, video.readableBytes());
		}
	}
	
	private void submitTask(String key, long startTimestamp, long endTimestamp, boolean doCompression) {
		Instant from = Instant.ofEpochMilli(startTimestamp);
		Instant to = Instant.ofEpochMilli(endTimestamp);
		long timeout = ChronoUnit.SECONDS.between(from, to) * 2;
		
		Runnable videoKeepTask = new VideoKeepTask(key, doCompression);
		Future<?> future = executor.submit(videoKeepTask);
		ScheduledFuture<?> scheduledFuture = timeoutService.schedule(new TimeoutCheckTask(future, videoKeepTask), timeout, TimeUnit.SECONDS);
		executorTimeoutMap.put(videoKeepTask, scheduledFuture);
	}

	protected abstract void doKeep(String key, byte[] data) throws Exception;
	
	protected abstract void doCleanup(Date lastCleanup) throws Exception;
	
	/**
	 * Runnable to do keeping and cleanup task
	 * 
	 * @author pabloc
	 *
	 */
	@VisibleForTesting
	class VideoKeepTask implements Runnable {

		private String key;
		private boolean doCompression;

		public VideoKeepTask(String key, boolean doCompression) {
			super();
			this.key = key;
			this.doCompression = doCompression;
		}
		
		@Override
		public void run() {
			logger.info("Video keep task started for keeper {} with id {}", getID(), key);
			byte[] data = null;
			try {
				logger.trace("Get key {} from memcached", key);
				data = (byte[]) memcachedClient.get(key); //O lo logro guardar o chau
				logger.trace("Got key {} from memcached", key);
				memcachedClient.delete(key);
				logger.trace("Deleted key {} from memcached", key);
			} catch (Exception e) {
				logger.error("Failed getting key {} from memcached client", e, key);
				cancelTimeoutTask(e);
			}
			if (data != null && data.length > 0) {
				Instant begin = Instant.now();
				String extension = ".264";
				if (doCompression) {
					ByteBuf compressedBuffer = null;
					try {
						compressedBuffer = compressVideo(data);
						data = new byte[compressedBuffer.readableBytes()];
						compressedBuffer.readBytes(data);
						extension = ".mkv";
					} catch (Exception e) {
						logger.error("Failed compressing video of size {} bytes. Fallback to raw h264", e, data.length);
					} finally {
						if (compressedBuffer != null) {
							compressedBuffer.release();
						}
					}
				}
				try {
					doKeep(key + extension, data);
					logger.info("Keeping of file with {} keeper took {} seconds", getID(), ChronoUnit.SECONDS.between(begin, Instant.now()));
				} catch (Exception e) {
					logger.error("Keeping of file with keeper {} and key {} failed. Data size lost: {} bytes", e, getID(), key, data.length);
					cancelTimeoutTask(e);
				} finally {
					data = null;
				}
			}
			if (!lock) {
				lock = true;
				Date lastCleanup = null;
				try {
					lastCleanup = (Date) memcachedClient.get(getID() + KEY_LAST_CLEANUP);
					checkDateAndCleanup(lastCleanup);
				} catch (Exception e) {
					logger.error("Failed executing cleanup with keeper {}", e, getID());
					cancelTimeoutTask(e);
				}
				lock = false;
			}
			cancelTimeoutTask(null);
			logger.info("Video keep task finished for keeper {}", getID());
		}

		private ByteBuf compressVideo(byte[] data) throws Exception {
			logger.debug("Start compression of {} bytes of video", data.length);
			FFmpegFrameGrabber frameGrabber = null;
			FFmpegFrameRecorder frameRecorder = null;
			InputStream is = new ByteArrayInputStream(data);
			ByteBuf buffer = byteBufAllocator.buffer();
			OutputStream outputStream = new ByteBufOutputStream(buffer);
			try {
				logger.trace("Join try catch block");
				frameGrabber = new FFmpegFrameGrabber(is);
				frameGrabber.setFormat("h264");
				frameGrabber.setAudioChannels(0);
				frameRecorder = new FFmpegFrameRecorder(outputStream, 0);
				
				logger.trace("Created objects");
				frameGrabber.start();
				logger.trace("Started frame grabber");
				frameRecorder.setFormat("matroska");
				frameRecorder.setImageHeight(frameGrabber.getImageHeight());
				frameRecorder.setImageWidth(frameGrabber.getImageWidth());
				frameRecorder.setVideoBitrate(videoBitrate);
				frameRecorder.setVideoCodec(avcodec.AV_CODEC_ID_MPEG4);
				frameRecorder.start();
				logger.trace("Started frame recorder");
				Frame frame = null;
				while ((frame = frameGrabber.grab()) != null) {
					frameRecorder.record(frame);
				}
				logger.info("compressed {} bytes to {} bytes", data.length, buffer.readableBytes());
				return buffer;
			} catch (Error err) {
				logger.error("Fail", err);
				throw err;
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
				try {
					outputStream.close();
				} catch (IOException e) {
					logger.error("Failed closing outputStream resource", e);
				}
				try {
					is.close();
				} catch (IOException e) {
					logger.error("Failed closing inputStream resource", e);
				}
				is = null;
				outputStream = null;
				frameGrabber = null;
				frameRecorder = null;
				logger.debug("Finished closing compression resources");
			}
		}

		private void checkDateAndCleanup(Date lastCleanup) throws Exception {
			Instant begin = Instant.now();
			if (lastCleanup != null) {
				long days = ChronoUnit.DAYS.between(ZonedDateTime.ofInstant(lastCleanup.toInstant(), ZoneId.systemDefault()), 
						ZonedDateTime.now());
				if (days >= 1) {
					doCleanup(lastCleanup);
					memcachedClient.replace(getID() + KEY_LAST_CLEANUP, 3600 * 24 * 2, new Date());
					logger.info("Cleanup with {} keeper took {} seconds", getID(), ChronoUnit.SECONDS.between(begin, Instant.now()));
				}
			} else {
				doCleanup(lastCleanup);
				memcachedClient.set(getID() + KEY_LAST_CLEANUP, 3600 * 24 * 2, new Date());
				logger.info("Cleanup with {} keeper took {} seconds", getID(), ChronoUnit.SECONDS.between(begin, Instant.now()));
			}
		}
		
		private void cancelTimeoutTask(Throwable t) {
			if (executorTimeoutMap.containsKey(this)) {
				try {
					boolean result = executorTimeoutMap.get(this).cancel(true);
					logger.debug("Cancellation of timeout task due to {} termination of video keep task of keeper {} returned with result {}", t == null ? "successful" : "failed", getID(), result);
					executorTimeoutMap.remove(this);
				} catch (Exception e) {
					logger.error("Failed while cancelling timeout task", e);
				}
			} else {
				logger.debug("Could not find scheduled timeout task for video keep task of keeper {}", getID());
			}
		}
	}

	private class TimeoutCheckTask implements Runnable {

		private Future<?> future;
		private Runnable videoKeepTask;

		TimeoutCheckTask(Future<?> future, Runnable videoKeepTask) {
			super();
			this.future = future;
			this.videoKeepTask = videoKeepTask;
		}
		
		@Override
		public void run() {
			if (future == null) {
				return;
			}
			if (!future.isDone()) {
				logger.debug("Try to cancel task that took too long of keeper {}", getID());
				boolean result = future.cancel(true);
				logger.debug("Cancellation of task that took too long of keeper {} resulted? {}", getID(), result);
				executorTimeoutMap.remove(videoKeepTask);
			}
		}
	}
}
