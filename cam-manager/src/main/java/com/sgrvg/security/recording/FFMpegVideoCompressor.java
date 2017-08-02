package com.sgrvg.security.recording;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

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

public class FFMpegVideoCompressor implements VideoCompressor {

	private SimpleLogger logger;
	private ByteBufAllocator byteBufAllocator;
	
	private Executor executor;

	@Inject
	public FFMpegVideoCompressor(SimpleLogger logger, ByteBufAllocator byteBufAllocator) {
		super();
		this.logger = logger;
		this.byteBufAllocator = byteBufAllocator;
		this.executor = Executors.newFixedThreadPool(4, new ThreadFactory() {
			final ThreadFactory delegate = Executors.defaultThreadFactory();
			
			@Override
			public Thread newThread(Runnable r) {
				final Thread t = delegate.newThread(r);
				t.setDaemon(true);
				t.setName(""); //TODO
				return t;
			}
		});
	}
	
	@Override
	public void compressAndExecute(ByteBuf data, int bitrate, long startTime, long endTime, VideoKeeper... keepers) {
		
	}

	@Override
	public byte[] compress(ByteBuf data) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private class CompressionTask implements Runnable {

		private byte[] data;
		private int bitrate;
		private long startTime;
		private long endTime;

		CompressionTask(byte[] data, int bitrate, long startTime, long endTime, VideoKeeper[] keepers) {
			super();
			this.data = data;
			this.bitrate = bitrate;
			this.startTime = startTime;
			this.endTime = endTime;
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			
		}
		
		private byte[] doCompression(byte[] data, int bitrate) {
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
	}
}
