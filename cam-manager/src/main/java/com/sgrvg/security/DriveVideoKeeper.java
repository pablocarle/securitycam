package com.sgrvg.security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Date;
import java.util.Properties;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.drive.Drive;
import com.google.inject.Inject;

import net.spy.memcached.MemcachedClient;

/**
 * @author pabloc
 *
 */
public class DriveVideoKeeper extends AbstractVideoKeeper {

	private static final String JSON_CREDENTIAL_KEY = "key_location";
	
	private GoogleCredential credential;
	{
		try (InputStream is = getClass().getClassLoader().getResourceAsStream("general.properties")) {
			Properties props = new Properties();
			props.load(is);
			String keyLocation = props.getProperty(JSON_CREDENTIAL_KEY);
			if (Strings.isNullOrEmpty(keyLocation)) {
				throw new RuntimeException("No Google KEY Location Defined");
			} else {
				credential = GoogleCredential.fromStream(
						new ByteArrayInputStream(
								Files.readAllBytes(Paths.get(new URI("file://" + keyLocation))
										)
								)
						);
			}
		} catch (IOException | URISyntaxException e) {
			logger.error("Failed to initialize google credentials", e);
		}
	}
	
	@Inject
	public DriveVideoKeeper(MemcachedClient memcachedClient, SimpleLogger logger) {
		super(memcachedClient, logger);
	}

	@Override
	protected void doKeep(String key, byte[] data) {
		if (credential == null) {
			logger.warn("GOOGLE CREDENTIAL NOT INITIALIZED");
			return;
		}
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
