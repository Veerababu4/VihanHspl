package com.vihan.hospital.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application settings under the {@code app.*} prefix (see application.yml).
 *
 * @param sampleData         true when the database holds fictional sample data (local profile)
 * @param corsAllowedOrigins browser origins allowed to call the API
 * @param uploadDir          folder for uploaded images (doctor photos, branch images, gallery)
 * @param publicFileBaseUrl  URL prefix the browser uses for uploaded files, e.g. {@code /files}
 */
@ConfigurationProperties("app")
public record AppProperties(boolean sampleData, List<String> corsAllowedOrigins, String uploadDir,
		String publicFileBaseUrl) {
}
