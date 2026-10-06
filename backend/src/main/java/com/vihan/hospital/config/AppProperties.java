package com.vihan.hospital.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application settings under the {@code app.*} prefix (see application.yml).
 *
 * @param sampleData         true when the database holds fictional sample data (local profile)
 * @param corsAllowedOrigins browser origins allowed to call the API
 */
@ConfigurationProperties("app")
public record AppProperties(boolean sampleData, List<String> corsAllowedOrigins) {
}
