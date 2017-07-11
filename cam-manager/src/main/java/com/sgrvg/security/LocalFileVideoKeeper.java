package com.sgrvg.security;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Inject;

import net.spy.memcached.MemcachedClient;

/**
 * Video keeper that saves videos to local filesystem.
 * 
 * @author pabloc
 *
 */
public class LocalFileVideoKeeper extends AbstractVideoKeeper {
	
	private final String basePath;
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
		basePath = props.getProperty("videos_base_path", "/home/alarm");
	}

	@Inject
	public LocalFileVideoKeeper(MemcachedClient memcachedClient, SimpleLogger logger) {
		super(memcachedClient, logger);
	}

	@Override
	protected void doKeep(String key, byte[] data) {
		final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");
		try {
			final String path = "file://" + basePath + "/" + SDF.format(new Date());
			final String filePath = path + "/" + key + ".264";
			Path directory = Paths.get(new URI(path));
			if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
				Files.createDirectory(directory);
				logger.info("Created directory {}", directory);
			}
			Files.write(Paths.get(new URI("file://" + filePath)), data, StandardOpenOption.CREATE);
		} catch (IOException | URISyntaxException e) {
			logger.error("Failed to save local file with key {}. Data size lost: {} bytes", e, key, data.length);
		}
	}

	@Override
	protected void doCleanup(Date lastCleanup) {
		// TODO Calcular
		if (lastCleanup == null) {
			
		} else {
			
		}
	}
	
	private void doCleanup(Instant from, Instant to) {
		try (Stream<Path> paths = Files.find(Paths.get(new URI("file://" + basePath)), 2, 
					(path, attrs) -> {
						Instant creationTime = attrs.creationTime().toInstant();
						return String.valueOf(path).endsWith(".264") && creationTime.isAfter(from) && creationTime.isBefore(to);
					})) {
			List<Boolean> results = paths
					.filter(path -> !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
					.flatMap(file -> {
						try {
							return Stream.of(Boolean.valueOf(Files.deleteIfExists(file)));
						} catch (IOException e) {
							logger.warn("Failed to delete file {}", e, file);
							return Stream.of(false);
						}
					})
					.collect(Collectors.toList());
				
			logger.info("{} Files successfully deleted", results.stream().mapToInt(result -> result ? 1 : 0).sum());
			logger.info("{} Files failed to delete", results.stream().mapToInt(result -> result ? 0 : 1).sum());
		} catch (IOException | URISyntaxException e) {
			logger.error("Failed to delete files in period between {} and {}", e, from, to);
		}
	}
}
