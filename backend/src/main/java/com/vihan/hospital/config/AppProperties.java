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
 * @param scheduling         slot generation settings (DESIGN.md §10.2)
 * @param booking            public booking rules (DESIGN.md §5.2, §17)
 * @param captcha            Cloudflare Turnstile verification (DESIGN.md §19.2)
 * @param rateLimit          per-IP request limits on public booking endpoints (DESIGN.md §12.4)
 */
@ConfigurationProperties("app")
public record AppProperties(boolean sampleData, List<String> corsAllowedOrigins, String uploadDir,
		String publicFileBaseUrl, Auth auth, Scheduling scheduling,
		Booking booking, Captcha captcha, RateLimit rateLimit) {

	/**
	 * @param jwtSecret              HMAC key for access tokens; at least 32 characters
	 * @param bootstrapAdminPassword creates the first IT Admin ("admin") when none exists; blank = skip
	 * @param devSamplePassword      local profile only: password for the sample staff logins; blank = skip
	 */
	public record Auth(String jwtSecret, int accessTokenMinutes, int refreshTokenHours, int maxFailedAttempts,
			int lockMinutes, boolean cookieSecure, String bootstrapAdminPassword, String devSamplePassword) {
	}

	/**
	 * @param windowDays        rolling window kept generated (14 = today + 13 days)
	 * @param maxGenerateDays   furthest ahead the manual "Generate" may go
	 * @param generateOnStartup reconcile all doctors when the app starts
	 */
	public record Scheduling(int windowDays, int maxGenerateDays, boolean generateOnStartup) {
	}

	public record Booking(int minLeadMinutes, int cancelCutoffMinutes, int maxReschedules, int maxPerMobilePerDay) {
	}

	/** @param expectedHostname if set, the captcha must have been solved on this host */
	public record Captcha(boolean enabled, String secretKey, String expectedHostname) {
	}

	public record RateLimit(int bookingPerMinute, int lookupPerMinute, int slotsPerMinute, int loginPerMinute) {
	}

}
