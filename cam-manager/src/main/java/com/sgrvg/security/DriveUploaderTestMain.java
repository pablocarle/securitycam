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
import java.text.MessageFormat;
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
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import com.google.common.base.Strings;

public class DriveUploaderTestMain {

	private static final String MAIN_FOLDER_ID = "0B4ZhPs6AV4VJLTgxM3B3QTBrY2s";

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
		
		try {
			
			Drive.Files.List listRequest = drive.files().list();
			FileList fileList = listRequest.setQ("'" + MAIN_FOLDER_ID + "' in parents")
					.setFields("nextPageToken, files(id, name)")
					.setSpaces("drive")
					.execute();
			
			if (!fileList.isEmpty()) {
				System.out.println("Encontre " + fileList.size() + " carpetas.");
				fileList.getFiles()
					/*.forEach(file -> {
						try {
							drive.permissions()
							.create(
									file.getId(),
									new Permission()
									.setEmailAddress("gesell.cam.manager@gmail.com")
									.setExpirationTime(new DateTime(
											new Date(Instant.now().plus(3, ChronoUnit.DAYS).toEpochMilli())))
									.setRole("reader")
									.setType("user")
									)
							.execute();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}*/
					.forEach(file -> {
						System.out.println(MessageFormat.format("file: {1}", file.getName()));
					});
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
