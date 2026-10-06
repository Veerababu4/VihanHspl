package com.vihan.hospital.config;

import java.time.Duration;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.vihan.hospital.storage.LocalFileStorage;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final LocalFileStorage storage;

	public WebConfig(LocalFileStorage storage) {
		this.storage = storage;
	}

	/** Serves uploaded images. File names are random and never reused, so they can be cached for long. */
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/files/**")
			.addResourceLocations(storage.root().toUri().toString())
			.setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
	}

}
