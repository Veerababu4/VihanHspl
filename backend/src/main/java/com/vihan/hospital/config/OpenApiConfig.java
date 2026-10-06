package com.vihan.hospital.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI at /swagger-ui — enabled only in the local profile. */
@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI vihanOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Vihan Multi Speciality Hospital API")
				.version("Phase 1")
				.description("Public website, online booking and staff portal API"));
	}

}
