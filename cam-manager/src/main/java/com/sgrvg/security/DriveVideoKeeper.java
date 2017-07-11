package com.sgrvg.security;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Date;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.inject.Inject;

import net.spy.memcached.MemcachedClient;

/**
 * @author pabloc
 *
 */
public class DriveVideoKeeper extends AbstractVideoKeeper {

	@Inject
	public DriveVideoKeeper(MemcachedClient memcachedClient, SimpleLogger logger) {
		super(memcachedClient, logger);
	}

	@Override
	protected void doKeep(String key, byte[] data) {
		try {
			Drive drive = new Drive(GoogleNetHttpTransport.newTrustedTransport(), 
					new GsonFactory(), 
					null);
			
			drive.files().create(null, new ByteArrayContent(null, data));
			
			data = null;
			
		} catch (GeneralSecurityException | IOException e) {
			logger.error("Failed to initialize Drive object for key {}. Data size lost: {} bytes", e, key, data.length);
		}
	}
	
	private boolean isAuthenticated() {
		return false;
	}

	@Override
	protected void doCleanup(Date lastCleanup) {
		// TODO Auto-generated method stub
		
	}
	
	private void doCleanup(Instant from, Instant to) {
		
	}
}
