package com.vihan.hospital.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.vihan.hospital.config.AppProperties;

/** Saves files under {@code app.upload-dir}; they are served at {@code /files/**} by {@code WebConfig}. */
@Service
public class LocalFileStorage implements FileStorageService {

	private static final Logger log = LoggerFactory.getLogger(LocalFileStorage.class);

	private final Path root;

	public LocalFileStorage(AppProperties props) {
		this.root = Path.of(props.uploadDir()).toAbsolutePath().normalize();
		try {
			Files.createDirectories(root);   // must exist so /files/** maps to a directory URI
		}
		catch (IOException e) {
			throw new UncheckedIOException("Cannot create upload directory " + root, e);
		}
	}

	public Path root() {
		return root;
	}

	@Override
	public void save(String key, byte[] content) {
		Path target = resolve(key);
		try {
			Files.createDirectories(target.getParent());
			// Write to a temp file first so a crash never leaves a half-written image behind.
			Path temp = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
			Files.write(temp, content);
			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Could not save file " + key, e);
		}
	}

	@Override
	public void delete(String key) {
		try {
			Files.deleteIfExists(resolve(key));
		}
		catch (IOException e) {
			log.warn("Could not delete file {}: {}", key, e.getMessage());
		}
	}

	private Path resolve(String key) {
		Path path = root.resolve(key).normalize();
		if (!path.startsWith(root)) {
			throw new IllegalArgumentException("Invalid file key: " + key);   // path traversal guard
		}
		return path;
	}

}
