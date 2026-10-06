package com.vihan.hospital.storage;

import org.springframework.stereotype.Component;

import com.vihan.hospital.config.AppProperties;

/**
 * Turns a stored file key into the URL the browser uses. The DB keeps only the key
 * ({@code doctors/ab12.jpg}), so moving servers or domains is an .env change (DESIGN.md §16.2).
 * Values that are already paths or URLs (e.g. local placeholder images) pass through unchanged.
 */
@Component
public class FileUrls {

	private final String baseUrl;

	public FileUrls(AppProperties props) {
		String base = props.publicFileBaseUrl();
		this.baseUrl = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
	}

	public String toUrl(String stored) {
		if (stored == null || stored.isBlank()) {
			return null;
		}
		if (stored.startsWith("/") || stored.startsWith("http://") || stored.startsWith("https://")) {
			return stored;
		}
		return baseUrl + "/" + stored;
	}

}
