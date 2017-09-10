package com.sgrvg.security.recording;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
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
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.sgrvg.security.SimpleLogger;

import io.netty.buffer.ByteBufAllocator;
import net.spy.memcached.MemcachedClient;

/**
 * @author pabloc
 *
 */
public final class DriveVideoKeeper extends AbstractVideoKeeper {

	private static final String JSON_CREDENTIAL_KEY = "key_location";

	private final int backupDays;
	{
		Properties props = new Properties();
		InputStream is = getClass().getClassLoader().getResourceAsStream("general.properties");
		try {
			if (is == null) {
				is = new FileInputStream(new java.io.File("conf/general.properties"));
			}
			props.load(is);
		} catch (IOException e) {
			logger.error("Failed to get default config. Defaults to home directory", e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					logger.info("Failed to close input stream", e);
				}
			}
		}
		backupDays = Integer.valueOf(props.getProperty("remote_backup_days", "5"));
	}

	private GoogleCredential credential;
	{
		InputStream is = getClass().getClassLoader().getResourceAsStream("general.properties");
		try {
			if (is == null) {
				is = new FileInputStream(new java.io.File("conf/general.properties"));
			}
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
				credential = credential.createScoped(Collections.singleton(DriveScopes.DRIVE));
			}
		} catch (IOException | URISyntaxException | GeneralSecurityException e) {
			logger.error("Failed to initialize google credentials", e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					logger.error("Failed while closing resource", e);
				}
			}
		}
	}

	private Drive drive;
	{
		try {
			drive = new Drive(GoogleNetHttpTransport.newTrustedTransport(), 
					new GsonFactory(), credential);
			About about = drive.about().get().setFields("storageQuota, user").execute();
			logger.info("*************** DRIVE ***************\n"
					+ "Current User Name: {}\n"
					+ "Total quota (Mbytes): {}\n"
					+ "Used quota (Mbytes): {}\n"
					+ "Used quota of trash (Mbytes): {}\n"
					+ "Free space (Mbytes): {}", about.getUser().getEmailAddress() + "|" + about.getUser().getDisplayName(),
					(about.getStorageQuota().getLimit() / 1024) / 1024,
					(about.getStorageQuota().getUsageInDrive() / 1024) / 1024,
					(about.getStorageQuota().getUsageInDriveTrash() / 1024) / 1024,
					((about.getStorageQuota().getLimit() - about.getStorageQuota().getUsageInDrive() - about.getStorageQuota().getUsageInDriveTrash()) / 1024) / 1024);

		} catch (GeneralSecurityException | IOException e) {
			logger.error("Failed to initialize DRIVE SERVICE", e);
		}
	}


	/**
	 * Constructs a new Drive Video Keeper
	 * 
	 * @param memcachedClient Instance of memcached.
	 * @param logger Implementation of SimpleLogger
	 * @param byteBufAllocator Allocator used to get memory buffers for compression
	 * @param doCompression If it keeps videos after or before compression
	 * @param videoBitrate Bitrate used in compression in bytes/s
	 */
	@Inject
	public DriveVideoKeeper(MemcachedClient memcachedClient, SimpleLogger logger, ByteBufAllocator byteBufAllocator, int videoBitrate) {
		super(memcachedClient, logger, byteBufAllocator, videoBitrate);
	}

	@Override
	protected void doKeep(String key, byte[] data) throws Exception {
		if (credential == null) {
			logger.warn("GOOGLE CREDENTIAL NOT INITIALIZED");
			return;
		}
		if (drive == null) {
			logger.warn("DRIVE SERVICE IS NOT INITIALIZED");
			return;
		}
		File folder = findTodaysFolder().map(file -> file).orElseGet(this::createTodayFolder);

		File fileMetadata = new File();
		fileMetadata.setName(key);
		fileMetadata.setParents(Collections.singletonList(folder.getId()));
		Drive.Files.Create createRequest = drive.files().create(fileMetadata , new ByteArrayContent("video/H264", data));
		createRequest.getMediaHttpUploader().setProgressListener(listener -> {
			if (listener == null) return;
			String status = null;
			switch (listener.getUploadState()) {
			case INITIATION_STARTED:
				status = "Initiation started!";
				break;
			case INITIATION_COMPLETE:
				status = "Initiation completed!";
				break;
			case MEDIA_IN_PROGRESS:
				double percent = listener.getProgress() * 100;
				status = "In Progress";
				if (logger.isDebugEnabled()) {
					logger.debug("Progress for key {} is {}%", key, String.valueOf(percent));
				}
				break;
			case MEDIA_COMPLETE:
				status = "Upload is complete!";
				break;
			case NOT_STARTED:
				status = "Upload has not started yet";
				break;
			default:
				status = "Unknown status";
				break;
			}
			logger.info("Progress for upload of file {} is {}", key, status);
		});
		createRequest.execute();
		data = null;
	}

	private Optional<File> findTodaysFolder() {
		final String todayFolder = getTodayFolderName();
		try {
			Drive.Files.List listRequest = drive.files().list();
			FileList fileList = listRequest.setQ("mimeType='application/vnd.google-apps.folder' and name = '" + todayFolder + "'")
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
	protected void doCleanup(Date lastCleanup) throws Exception {
		Instant from = Instant.now().minus(Period.ofDays(30));
		Instant to = Instant.now().minus(Period.ofDays(backupDays));
		logger.info("Delete files from Drive from {} to {}", from, to);
		doCleanup(from, to);
	}

	private void doCleanup(Instant from, Instant to) {
		FileList filesToDelete = findFilesToDelete(from, to);
		logger.info("Found {} files to delete from Drive", filesToDelete.getFiles().size());
		filesToDelete.getFiles().stream().forEach(this::deleteFile);
	}

	private FileList findFilesToDelete(Instant from, Instant to) {
		try {
			Drive.Files.List listRequest = drive.files().list();
			String q = "modifiedTime >= '" + formatInstant(from) 
			+ "' and modifiedTime <= '" + formatInstant(to) + "' and mimeType='application/vnd.google-apps.folder'";
			logger.debug("Search for files with Q {}", q);
			return listRequest.setQ(q)
					.setFields("nextPageToken, files(id, name)")
					.setSpaces("drive")
					.execute();
		} catch (IOException e) {
			throw new DriveException(e);
		}
	}

	private String formatInstant(Instant to) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
		return sdf.format(new Date(to.toEpochMilli()));
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
