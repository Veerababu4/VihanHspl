package com.vihan.hospital;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;

import com.vihan.hospital.captcha.CaptchaVerifier;
import com.vihan.hospital.common.error.ApiException;
import com.vihan.hospital.config.AppProperties;

/**
 * Integration tests don't call Cloudflare: this stand-in accepts the token {@value #PASS} and
 * rejects anything else, so the captcha paths are still exercised. (The real HTTP check is
 * covered by CaptchaVerifierTest with a mock Cloudflare.)
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestCaptchaConfiguration {

	public static final String PASS = "pass";

	@Bean
	@Primary
	CaptchaVerifier testCaptchaVerifier(AppProperties props) {
		return new CaptchaVerifier(props) {
			@Override
			public void verify(String token, String remoteIp) {
				if (token == null || token.isBlank()) {
					throw new ApiException(HttpStatus.BAD_REQUEST, "CAPTCHA_REQUIRED", "captcha required");
				}
				if (!PASS.equals(token)) {
					throw new ApiException(HttpStatus.BAD_REQUEST, "CAPTCHA_FAILED", "captcha failed");
				}
			}
		};
	}

}
