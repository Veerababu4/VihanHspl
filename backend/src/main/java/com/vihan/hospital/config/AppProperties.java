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
 * @param auth               staff login settings (DESIGN.md §12.2)
 */
@ConfigurationProperties("app")
public record AppProperties(boolean sampleData, List<String> corsAllowedOrigins, String uploadDir,
		String publicFileBaseUrl, Auth auth) {

	/**
	 * @param jwtSecret              HMAC key for access tokens; at least 32 characters
	 * @param bootstrapAdminPassword creates the first IT Admin ("admin") when none exists; blank = skip
	 * @param devSamplePassword      local profile only: password for the sample staff logins; blank = skip
	 */
	public record Auth(String jwtSecret, int accessTokenMinutes, int refreshTokenHours, int maxFailedAttempts,
			int lockMinutes, boolean cookieSecure, String bootstrapAdminPassword, String devSamplePassword) {
	}

}
