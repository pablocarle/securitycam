package com.sgrvg.security;

import java.io.IOException;
import java.security.GeneralSecurityException;

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
			
		} catch (GeneralSecurityException | IOException e) {
			logger.error("Failed to initialize Drive object", e);
		}
	}
	
	private boolean isAuthenticated() {
		return false;
	}
}
