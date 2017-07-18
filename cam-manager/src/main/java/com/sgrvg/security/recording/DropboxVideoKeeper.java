package com.sgrvg.security.recording;

import java.util.Date;

import com.sgrvg.security.SimpleLogger;

import net.spy.memcached.MemcachedClient;

/**
 * Implements Dropbox API to upload files
 * 
 * @author pabloc
 *
 */
public class DropboxVideoKeeper extends AbstractVideoKeeper {

	public DropboxVideoKeeper(MemcachedClient memcachedClient, SimpleLogger logger, boolean doCompression) {
		super(memcachedClient, logger, doCompression);
	}

	@Override
	protected void doKeep(String key, byte[] data) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	protected void doCleanup(Date lastCleanup) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public String getID() {
		throw new RuntimeException("Not implemented");
	}
}
