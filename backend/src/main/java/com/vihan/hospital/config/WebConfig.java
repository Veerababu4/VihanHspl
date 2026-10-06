package com.vihan.hospital.config;

import java.time.Duration;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.vihan.hospital.common.web.RateLimitInterceptor;
import com.vihan.hospital.storage.LocalFileStorage;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final LocalFileStorage storage;
	private final RateLimitInterceptor rateLimits;

	public WebConfig(LocalFileStorage storage, RateLimitInterceptor rateLimits) {
		this.storage = storage;
		this.rateLimits = rateLimits;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(rateLimits).addPathPatterns("/api/public/**", "/api/auth/login");
	}

	/** Serves uploaded images. File names are random and never reused, so they can be cached for long. */
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/files/**")
			.addResourceLocations(storage.root().toUri().toString())
			.setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
	}

}
