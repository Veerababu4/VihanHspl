package com.vihan.hospital;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// No default in-memory user: staff authentication (JWT) arrives in M3.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class VihanHospitalApplication {

	/**
	 * All business times are IST (DESIGN.md §9.1). Pinned in code rather than read from the
	 * environment, because servers and containers often run with TZ=UTC.
	 */
	public static final String TIME_ZONE = "Asia/Kolkata";

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(TIME_ZONE));
		SpringApplication.run(VihanHospitalApplication.class, args);
	}

}
