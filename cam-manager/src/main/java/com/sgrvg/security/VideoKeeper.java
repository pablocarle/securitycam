package com.sgrvg.security;

import io.netty.buffer.ByteBuf;

/**
 * Interface to keep data in several places
 * 
 * @author pabloc
 *
 */
public interface VideoKeeper {

	/**
	 * Perform video keeping async task
	 * 
	 * @param startTimestamp Timestamp covered by video
	 * @param endTimestamp Timestamp covered by video
	 * @param name Descriptive name of source
	 * @param video Video data buffer
	 * @param doCompression Weather to perform a compression task or not
	 */
	void keep(long startTimestamp, long endTimestamp, String name, ByteBuf video, boolean doCompression);
	
	/**
	 * Get a unique ID of videoKeeper
	 * 
	 * @return ID
	 */
	String getID();

}
