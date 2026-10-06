package com.vihan.hospital.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.vihan.hospital.VihanHospitalApplication;
import com.vihan.hospital.config.AppProperties;

/** Access tokens: HS256 JWTs signed with JWT_SECRET (DESIGN.md §12.2). */
@Configuration
public class JwtConfig {

	static final String CLAIM_ROLE = "role";
	static final String CLAIM_PASSWORD_CHANGE = "pwd_change";

	/** The only authority of a user who must change a temporary password first. */
	public static final String PASSWORD_CHANGE_REQUIRED = "PASSWORD_CHANGE_REQUIRED";

	private static final int MIN_SECRET_LENGTH = 32;   // HS256 needs a 256-bit key

	@Bean
	SecretKey jwtSigningKey(AppProperties props) {
		String secret = props.auth().jwtSecret();
		if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
			throw new IllegalStateException(
					"JWT_SECRET must be set to at least " + MIN_SECRET_LENGTH + " characters (see backend/.env.example)");
		}
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
		return NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
	}

	/**
	 * Token → authorities. A user with a temporary password gets only PASSWORD_CHANGE_REQUIRED,
	 * so every role-protected API refuses them until the password is changed (FR-A2).
	 */
	@Bean
	Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
		return jwt -> {
			var authority = Boolean.TRUE.equals(jwt.getClaimAsBoolean(CLAIM_PASSWORD_CHANGE))
					? new SimpleGrantedAuthority(PASSWORD_CHANGE_REQUIRED)
					: new SimpleGrantedAuthority("ROLE_" + jwt.getClaimAsString(CLAIM_ROLE));
			return new JwtAuthenticationToken(jwt, List.of(authority), jwt.getSubject());
		};
	}

	/** BCrypt cost 12 (DESIGN.md §12.2). */
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	/** Injected wherever "now" matters (lockout, token expiry) so tests can control time. */
	@Bean
	Clock clock() {
		return Clock.system(ZoneId.of(VihanHospitalApplication.TIME_ZONE));
	}

}
