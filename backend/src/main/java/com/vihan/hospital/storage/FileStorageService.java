package com.vihan.hospital.storage;

/**
 * Stores uploaded files by key (e.g. {@code doctors/3f2a….jpg}). Phase 1 writes to the server disk;
 * swapping in object storage later means a new implementation, not changes to callers (DESIGN.md §7).
 */
public interface FileStorageService {

	void save(String key, byte[] content);

	/** Deletes the file if it exists; missing files are ignored. */
	void delete(String key);

}
