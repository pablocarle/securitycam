package com.sgrvg.security.recording;

import com.sgrvg.security.VideoKeeper;

import io.netty.buffer.ByteBuf;

public interface VideoCompressor {

	void compressAndExecute(ByteBuf data, int bitrate, long startTime, long endTime, VideoKeeper... keepers);
	
	byte[] compress(ByteBuf data);  
}
