package com.sgrvg.security;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.google.common.base.Strings;

public class DriveUploaderTestMain {

	private GoogleCredential credential;
	{
		InputStream is = getClass().getClassLoader().getResourceAsStream("general.properties");
		try {
			if (is == null) {
				is = new FileInputStream(new java.io.File("conf/general.properties"));
			}
			Properties props = new Properties();
			props.load(is);
			String keyLocation = props.getProperty("key_location");
			if (Strings.isNullOrEmpty(keyLocation)) {
				throw new RuntimeException("No Google KEY Location Defined");
			} else {
				credential = GoogleCredential.fromStream(
						new ByteArrayInputStream(
								Files.readAllBytes(Paths.get(new URI("file://" + keyLocation))
										)
								), GoogleNetHttpTransport.newTrustedTransport(),
						new GsonFactory()
						);
				credential = credential.createScoped(Collections.singleton(DriveScopes.DRIVE));
			}
		} catch (IOException | URISyntaxException | GeneralSecurityException e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private Drive drive;
	{
		try {
			drive = new Drive(GoogleNetHttpTransport.newTrustedTransport(), 
					new GsonFactory(), credential);
		} catch (GeneralSecurityException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		
		new DriveUploaderTestMain().doIt();
		
	}
	
	private void doIt() {
		//Creo carpeta compartida
		File fileMetadata = new File();
		fileMetadata.setName("Test Folder 2");
		fileMetadata.setMimeType("application/vnd.google-apps.folder");
		
		try {
			File folder = drive.files().create(fileMetadata)
					.setFields("id")
					.execute();
			
			drive.permissions()
			.create(
					folder.getId(),
					new Permission()
					.setEmailAddress("pablo.carle@gmail.com")
					.setExpirationTime(new DateTime(
							new Date(Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli())))
					.setRole("reader")
					.setType("user")
					)
			.execute();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
