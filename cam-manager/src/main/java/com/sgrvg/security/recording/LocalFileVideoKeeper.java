package com.sgrvg.security.recording;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.Period;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Inject;
import com.sgrvg.security.SimpleLogger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import net.spy.memcached.MemcachedClient;

/**
 * Video keeper that saves videos to local filesystem.
 * 
 * @author pabloc
 *
 */
public class LocalFileVideoKeeper extends AbstractVideoKeeper {
	
	private final String basePath;
	private final int backupDays;
	{
		Properties props = new Properties();
		InputStream is = getClass().getClassLoader().getResourceAsStream("general.properties");
		try {
			if (is == null) {
				is = new FileInputStream(new File("conf/general.properties"));
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
		basePath = props.getProperty("videos_base_path", "/home/alarm");
		backupDays = Integer.valueOf(props.getProperty("local_backup_days", "5"));
	}

	/**
	 * Constructs a new Local File Video Keeper. It saves videos in local filesystem
	 * 
	 * @param memcachedClient Instance of memcached.
	 * @param logger Implementation of SimpleLogger
	 * @param byteBufAllocator Allocator used to get memory buffers for compression
	 * @param videoBitrate Bitrate used in compression in bytes/s
	 */
	@Inject
	public LocalFileVideoKeeper(MemcachedClient memcachedClient, SimpleLogger logger, ByteBufAllocator byteBufAllocator, int videoBitrate) {
		super(memcachedClient, logger, byteBufAllocator, videoBitrate);
	}

	@Override
	protected void doKeep(String key, ByteBuf data) throws Exception {
		final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");
		final String path = "file://" + basePath + "/" + SDF.format(new Date());
		final String filePath = path + "/" + key;
		Path directory = Paths.get(new URI(path));
		if (Files.notExists(directory, LinkOption.NOFOLLOW_LINKS)) {
			Files.createDirectory(directory);
			logger.info("Created directory {}", directory);
		}
		URI fileURI = new URI(filePath);
		long bytes = Files.copy(new ByteBufInputStream(data), Paths.get(fileURI), StandardCopyOption.REPLACE_EXISTING);
		logger.info("Written to file {}, {} bytes", fileURI, bytes);
	}

	@Override
	protected void doCleanup(Date lastCleanup) throws Exception {
		Instant from = Instant.now().minus(Period.ofDays(30));
		Instant to = Instant.now().minus(Period.ofDays(backupDays));
		doCleanup(from, to);
	}
	
	private void doCleanup(Instant from, Instant to) throws Exception {
		try (Stream<Path> paths = Files.find(Paths.get(new URI("file://" + basePath)), 2, 
					(path, attrs) -> {
						Instant creationTime = attrs.creationTime().toInstant();
						return (String.valueOf(path).endsWith(".264") || String.valueOf(path).endsWith(".mkv")) && creationTime.isAfter(from) && creationTime.isBefore(to);
					})) {
			List<Boolean> results = paths
					.filter(path -> !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
					.flatMap(file -> {
						try {
							return Stream.of(Files.deleteIfExists(file));
						} catch (IOException e) {
							logger.warn("Failed to delete file {}", e, file);
							return Stream.of(false);
						}
					})
					.collect(Collectors.toList());
				
			logger.info("{} Files successfully deleted", results.stream().mapToInt(result -> result ? 1 : 0).sum());
			logger.info("{} Files failed to delete", results.stream().mapToInt(result -> result ? 0 : 1).sum());
		}
	}

	@Override
	public String getID() {
		return this.getClass().getSimpleName();
	}
}
