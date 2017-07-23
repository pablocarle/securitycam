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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

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
	
	private ByteBufAllocator byteBufAllocator;
	private int videoBitrate;
	
	private volatile boolean lock = false;

	private final boolean doCompression;

	@Inject
	public AbstractVideoKeeper(
			MemcachedClient memcachedClient,
			SimpleLogger logger,
			ByteBufAllocator bytebufAllocator,
			boolean doCompression,
			int videoBitrate) {
		super();
		this.memcachedClient = memcachedClient;
		this.logger = logger;
		this.executor = Executors.newFixedThreadPool(5);
		this.doCompression = doCompression;
		this.byteBufAllocator = bytebufAllocator;
		this.videoBitrate = videoBitrate;
	}
	
	@Override
	public final void keep(long startTimestamp, long endTimestamp, String name, ByteBuf video) {
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
			executor.submit(new VideoKeepTask(key));
			logger.info("Submitted task with keeper {} and key {}", getID(), key);
		} catch (Exception e) {
			logger.error("Failed to keep video. {} bytes data lost", e, video.readableBytes());
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
			logger.info("Video keep task started for keeper {} with id {}", getID(), key);
			byte[] data = null;
			try {
				logger.debug("Get key {} from memcached", key);
				data = (byte[]) memcachedClient.get(key); //O lo logro guardar o chau
				logger.debug("Got key {} from memcached", key);
				memcachedClient.delete(key);
				logger.debug("Deleted key {} from memcached", key);
			} catch (Exception e) {
				logger.error("Failed getting key {} from memcached client", e, key);
			}
			if (data != null && data.length > 0) {
				Instant begin = Instant.now();
				String extension = ".264";
				if (doCompression) {
					data = compressVideo(data);
					extension = ".mkv";
				}
				try {
					doKeep(key + extension, data);
					logger.info("Keeping of file with {} keeper took {} seconds", getID(), ChronoUnit.SECONDS.between(begin, Instant.now()));
				} catch (Exception e) {
					logger.error("Unhandled exception from keeper {}", e, getID());
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
					logger.error("Failed executing cleanup of keeper {}", e, getID());
				}
				lock = false;
			}
			logger.info("Video keep task finished for keeper {}", getID());
		}

		private byte[] compressVideo(byte[] data) {
			logger.debug("Start compression of {} bytes of video", data.length);
			FFmpegFrameGrabber frameGrabber = null;
			FFmpegFrameRecorder frameRecorder = null;
			InputStream is = new ByteArrayInputStream(data);
			ByteBuf buffer = byteBufAllocator.buffer();
			OutputStream outputStream = new ByteBufOutputStream(buffer);
			try {
				logger.debug("Join try catch block");
				frameGrabber = new FFmpegFrameGrabber(is);
				frameGrabber.setFormat("h264");
				frameGrabber.setAudioChannels(0);
				frameRecorder = new FFmpegFrameRecorder(outputStream, 0);
				
				logger.debug("Created objects");
				frameGrabber.start();
				logger.debug("Started frame grabber");
				frameRecorder.setFormat("matroska");
				frameRecorder.setImageHeight(frameGrabber.getImageHeight());
				frameRecorder.setImageWidth(frameGrabber.getImageWidth());
				frameRecorder.setVideoBitrate(videoBitrate);
				frameRecorder.setVideoCodec(avcodec.AV_CODEC_ID_MPEG4);
				frameRecorder.start();
				logger.debug("Started frame recorder");
				Frame frame = null;
				while ((frame = frameGrabber.grab()) != null) {
					frameRecorder.record(frame);
				}
				byte[] outData = new byte[buffer.readableBytes()];
				buffer.readBytes(outData);
				logger.info("compressed {} bytes to {} bytes", data.length, outData.length);
				return outData;
			} catch (Exception e) {
				logger.error("Failed compressing video of size {} bytes. Fallback to raw h264", e, data.length);
				return data;
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
				boolean release = buffer.release();
				logger.debug("Release of compression buffer with result: {}", release);
				is = null;
				outputStream = null;
				frameGrabber = null;
				frameRecorder = null;
				logger.debug("Finished closing compression resources");
			}
		}

		private void checkDateAndCleanup(Date lastCleanup) {
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
	}
}
