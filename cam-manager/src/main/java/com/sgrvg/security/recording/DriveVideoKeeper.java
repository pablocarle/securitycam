package com.sgrvg.security.recording;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.sgrvg.security.SimpleLogger;

import net.spy.memcached.MemcachedClient;

/**
 * @author pabloc
 *
 */
public final class DriveVideoKeeper extends AbstractVideoKeeper {

	private static final String JSON_CREDENTIAL_KEY = "key_location";
	private static final String MAIN_FOLDER_ID = "0B4ZhPs6AV4VJLTgxM3B3QTBrY2s";
	
	private final int backupDays;
	{
		Properties props = new Properties();
		InputStream is = getClass().getClassLoader().getResourceAsStream("general.properties");
		try {
			props.load(is);
		} catch (IOException e) {
			logger.error("Failed to get default config. Defaults to home directory", e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				logger.info("Failed to close input stream", e);
			}
		}
		backupDays = Integer.valueOf(props.getProperty("remote_backup_days", "5"));
	}
	
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
								), GoogleNetHttpTransport.newTrustedTransport(),
						new GsonFactory()
						);
			}
		} catch (IOException | URISyntaxException | GeneralSecurityException e) {
			logger.error("Failed to initialize google credentials", e);
		}
	}
	
	private Drive drive;
	{
		 try {
			drive = new Drive(GoogleNetHttpTransport.newTrustedTransport(), 
						new GsonFactory(), null);
		} catch (GeneralSecurityException | IOException e) {
			logger.error("Failed to initialize DRIVE SERVICE", e);
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
		if (drive == null) {
			logger.warn("DRIVE SERVICE IS NOT INITIALIZED");
			return;
		}
		try {
			File folder = findTodaysFolder().map(file -> file).orElseGet(this::createTodayFolder);
			
			File fileMetadata = new File();
			fileMetadata.setFileExtension(".264");
			fileMetadata.setName(key);
			fileMetadata.setParents(Arrays.asList(MAIN_FOLDER_ID, folder.getId()));
			Drive.Files.Create createRequest = drive.files().create(fileMetadata , new ByteArrayContent("video/H264", data));
			createRequest.getMediaHttpUploader().setProgressListener(listener -> {
				logger.info("Progress for upload of file {} is {}", key);
			});
			createRequest.execute();
			data = null;
		} catch (IOException | DriveException e) {
			logger.error("File upload with key {} failed. Data size lost: {} bytes", e, key, data.length);
		}
	}
	
	private Optional<File> findTodaysFolder() {
		final String todayFolder = getTodayFolderName();
		try {
			Drive.Files.List listRequest = drive.files().list();
			FileList fileList = listRequest.setQ("mimeType='application/vnd.google-apps.folder' and name = '" + todayFolder + "'"
					+ " and '" + MAIN_FOLDER_ID + "' in parents")
			.setFields("nextPageToken, files(id, name)")
			.setSpaces("drive")
			.execute();
			if (!fileList.isEmpty()) {
				logger.info("Found today folder {}. There are {} other hits", todayFolder, fileList.size() - 1);
				return fileList.getFiles().stream().findFirst();
			} else {
				return Optional.empty();
			}
		} catch (IOException e) {
			throw new DriveException(e);
		}
	}
	
	private File createTodayFolder() {
		final String todayFolder = getTodayFolderName();
		File fileMetadata = new File();
		fileMetadata.setName(todayFolder);
		fileMetadata.setMimeType("application/vnd.google-apps.folder");
		fileMetadata.setParents(Collections.singletonList(MAIN_FOLDER_ID));
		
		try {
			return drive.files().create(fileMetadata)
					.setFields("id")
					.execute();
		} catch (IOException e) {
			throw new DriveException(e);
		}
	}

	private String getTodayFolderName() {
		return new SimpleDateFormat("yyyy-MM-dd").format(new Date()); 
	}

	@Override
	protected void doCleanup(Date lastCleanup) {
		Instant from = Instant.now().minus(Period.ofDays(30));
		Instant to = Instant.now().minus(Period.ofDays(backupDays));
		doCleanup(from, to);
	}
	
	private void doCleanup(Instant from, Instant to) {
		FileList filesToDelete = findFilesToDelete(from, to);
		filesToDelete.getFiles().stream().forEach(this::deleteFile);
	}

	private FileList findFilesToDelete(Instant from, Instant to) {
		try {
			Drive.Files.List listRequest = drive.files().list();
			return listRequest.setQ("'" + MAIN_FOLDER_ID + "' in parents and modifiedTime >= " + formatInstant(from) 
					+ " and modifiedTime <= " + formatInstant(to))
				.setFields("nextPageToken, files(id, name)")
				.setSpaces("drive")
				.execute();
		} catch (IOException e) {
			throw new DriveException(e);
		}
	}
	
	private String formatInstant(Instant to) {
		DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
														.withLocale(Locale.UK)
														.withZone(ZoneId.of("UTC"));
		return formatter.format(to);
	}

	private void deleteFile(File file) {
		try {
			Drive.Files.Delete deleteRequest = drive.files().delete(file.getId());
			deleteRequest.execute();
		} catch (IOException e) {
			throw new DriveException(e);
		}
	}

	@Override
	public String getID() {
		return this.getClass().getSimpleName();
	}
}