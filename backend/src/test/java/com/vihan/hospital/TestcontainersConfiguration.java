package com.vihan.hospital;

import java.time.Duration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.oracle.OracleContainer;

/**
 * A throwaway Oracle Database Free container for integration tests (needs Docker Desktop running).
 * Each test run gets an empty database, so Flyway migrations are tested from scratch.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	OracleContainer oracleContainer() {
		// The 4 GB Docker VM also runs the dev database, so start-up can take longer than the default.
		return new OracleContainer("gvenzl/oracle-free:23-slim-faststart")
			.withStartupTimeout(Duration.ofMinutes(5))
			.withStartupAttempts(2);
	}

}
