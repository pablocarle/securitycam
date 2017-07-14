package com.sgrvg.security;

import io.netty.buffer.ByteBuf;

/**
 * Interface to keep data in several places
 * 
 * @author pabloc
 *
 */
public interface VideoKeeper {

	void keep(long startTimestamp, long endTimestamp, String name, ByteBuf video);
	
	String getID();

}
