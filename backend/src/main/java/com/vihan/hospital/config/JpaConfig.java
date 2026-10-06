package com.vihan.hospital.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaConfig {

	/**
	 * Fills created_by / updated_by. Returns empty until staff login exists (M3), which will
	 * return the logged-in user's id from the security context.
	 */
	@Bean
	AuditorAware<Long> auditorAware() {
		return Optional::empty;
	}

}
