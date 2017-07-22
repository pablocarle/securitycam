package com.sgrvg.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

public class CompressionTestMain {

	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		final String basePath = "/home/pabloc/cam/2017-07-20/";
		final String fileName = "cam_3_1969-12-31_090000-2017-07-20_041101.264";
		final String outFileName = "output_" + System.currentTimeMillis() + ".mkv";
		FFmpegFrameGrabber frameGrabber = null;
		FFmpegFrameRecorder frameRecorder = null;
		avformat.AVFormatContext ifmt_ctx = null;
		
		try {
			ByteArrayInputStream is = new ByteArrayInputStream(Files.readAllBytes(Paths.get(new URI("file://" + basePath + fileName))));
			System.out.println("Input data is " + is.available() + " bytes");
			
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			//OutputStream stream = new BufferedOutputStream(outputStream);
			
			//OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(basePath + outFileName));
			
			//frameGrabber = new FFmpegFrameGrabber(basePath + fileName);
			frameGrabber = new FFmpegFrameGrabber(is);
			
			frameRecorder = new FFmpegFrameRecorder(outputStream, 0);
			//frameRecorder = new FFmpegFrameRecorder(basePath + outFileName, 0);
			//frameRecorder = new FFmpegFrameRecorder(new File("/home/pabloc/cam/tete.mp4"), 0);
			
			frameGrabber.setFormat("h264");
			//ifmt_ctx = avformat.avformat_alloc_context();
			
			avcodec.AVCodec codec = avcodec.av_codec_next(null);
			while (codec != null) {
				System.out.println("Codec with name " + codec.name().getString("UTF-8") + " and id " + codec.id());
				codec.close();
				codec = avcodec.av_codec_next(codec);
			}
			
			avformat.AVInputFormat format = avformat.av_iformat_next(null);
			while (format != null) {
				System.out.println("Input format with name " + format.name().getString("UTF-8") + " and id " + format.raw_codec_id());
				format.close();
				format = avformat.av_iformat_next(format);
			}
			
			//avcodec.AVCodec codec = avcodec.avcodec_find_decoder(avcodec.AV_CODEC_ID_H264);
			
			//avcodec.AVCodec codec = avcodec.avcodec_find_decoder(avcodec.AV_CODEC_ID_H264);
			//avformat.avformat_new_stream(ifmt_ctx, codec);
			//avformat.avformat_write_header(ifmt_ctx, options)
			
			frameGrabber.start();
			
			frameRecorder.setFormat("matroska");
			
			frameRecorder.setImageHeight(frameGrabber.getImageHeight());
			frameRecorder.setImageWidth(frameGrabber.getImageWidth());

			/*
			frameRecorder.setVideoOption("pix_fmt", "0");
			frameRecorder.setVideoOption("time_base", "1/1200000");
			frameRecorder.setVideoOption("crf", "23");
			frameRecorder.setVideoOption("sws_param", "flags=2");
			frameRecorder.setVideoOption("pixel_aspect", "0/1");
			*/
			
			//frameRecorder.setFrameRate(frameGrabber.getFrameRate());
			//frameRecorder.setVideoBitrate(frameGrabber.getVideoBitrate());

			frameRecorder.setVideoBitrate(1000000);
			frameRecorder.setVideoCodec(avcodec.AV_CODEC_ID_MPEG4);
			//frameRecorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
			//frameRecorder.setVideoCodecName("libx264");
			frameRecorder.start();
			
			
			Frame frame = null;
			while ((frame = frameGrabber.grab()) != null) {
				if (frameRecorder.getTimestamp() < frameGrabber.getTimestamp()) {
					frameRecorder.setTimestamp(frameGrabber.getTimestamp());
				}
				frameRecorder.record(frame.clone());
			}
			Files.write(Paths.get(new URI("file://" + basePath + outFileName)), outputStream.toByteArray(), StandardOpenOption.CREATE_NEW);
			System.out.println("Output data size is " + Files.readAllBytes(Paths.get(new URI("file://" + basePath + outFileName))).length + " bytes");
			System.out.println("Compression took " + (System.currentTimeMillis() - start) / 1000 + "seconds");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (frameGrabber != null) {
				try {
					frameGrabber.close();
				} catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
					e.printStackTrace();
				}
			}
			if (frameRecorder != null) {
				try {
					frameRecorder.close();
				} catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
					e.printStackTrace();
				}
			}
			if (ifmt_ctx != null) {
				avformat.avformat_free_context(ifmt_ctx);
			}
		}
		
	}

}
