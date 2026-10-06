package com.vihan.hospital.captcha;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.vihan.hospital.common.error.ApiException;
import com.vihan.hospital.config.AppProperties;

/**
 * Server-side check of a Cloudflare Turnstile token (DESIGN.md §19.2) on public booking, cancel and
 * reschedule. Tokens are single-use, so the browser needs a fresh one after every attempt.
 * If Cloudflare can't be reached the request is refused (fail closed) rather than letting bots in.
 */
@Component
public class CaptchaVerifier {

	static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
	private static final Logger log = LoggerFactory.getLogger(CaptchaVerifier.class);

	/** The part of Cloudflare's answer we use. */
	record SiteverifyResponse(boolean success, @JsonProperty("error-codes") List<String> errorCodes, String hostname) {
	}

	private final RestClient http;
	private final AppProperties.Captcha settings;

	@Autowired
	public CaptchaVerifier(AppProperties props) {
		this(RestClient.builder().requestFactory(requestFactory()), props);
	}

	/** For tests: lets a mock server stand in for Cloudflare. */
	CaptchaVerifier(RestClient.Builder builder, AppProperties props) {
		this.http = builder.build();
		this.settings = props.captcha();
		if (!settings.enabled()) {
			log.warn("Captcha checking is DISABLED (CAPTCHA_ENABLED=false). Never use this setting in the cloud.");
		}
		else if (settings.secretKey() == null || settings.secretKey().isBlank()) {
			log.error("TURNSTILE_SECRET_KEY is not set: every public booking will be refused");
		}
	}

	/**
	 * @param token    the token the Turnstile widget gave the browser
	 * @param remoteIp the visitor's IP, passed to Cloudflare as an extra signal
	 */
	public void verify(String token, String remoteIp) {
		if (!settings.enabled()) {
			return;
		}
		if (token == null || token.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "CAPTCHA_REQUIRED", "Please complete the \"I am human\" check");
		}
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("secret", settings.secretKey());
		form.add("response", token);
		if (remoteIp != null) {
			form.add("remoteip", remoteIp);
		}
		SiteverifyResponse answer;
		try {
			answer = http.post().uri(VERIFY_URL).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
				.retrieve().body(SiteverifyResponse.class);
		}
		catch (RestClientException e) {
			log.error("Captcha verification failed to reach Cloudflare: {}", e.getMessage());
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CAPTCHA_UNAVAILABLE",
					"We could not verify the \"I am human\" check right now. Please try again in a minute.");
		}
		if (answer == null || !answer.success()) {
			log.info("Captcha rejected: {}", answer == null ? "empty answer" : answer.errorCodes());
			throw captchaFailed();
		}
		String expectedHost = settings.expectedHostname();
		if (expectedHost != null && !expectedHost.isBlank() && !expectedHost.equalsIgnoreCase(answer.hostname())) {
			log.warn("Captcha solved on unexpected host {}", answer.hostname());
			throw captchaFailed();
		}
	}

	private static ApiException captchaFailed() {
		return new ApiException(HttpStatus.BAD_REQUEST, "CAPTCHA_FAILED",
				"The \"I am human\" check failed or expired. Please try it again.");
	}

	private static JdkClientHttpRequestFactory requestFactory() {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
		factory.setReadTimeout(Duration.ofSeconds(5));
		return factory;
	}

}
