package com.vihan.hospital.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.vihan.hospital.config.AppProperties;
import com.vihan.hospital.user.AppUser;

/** Issues short-lived access tokens (30 min). The browser keeps them in memory only. */
@Service
public class TokenService {

	static final String CLAIM_DOCTOR_ID = "doctor_id";
	private static final String ISSUER = "vihan-hospital";

	public record AccessToken(String value, Instant expiresAt) {
	}

	private final JwtEncoder encoder;
	private final Clock clock;
	private final Duration lifetime;

	public TokenService(JwtEncoder encoder, Clock clock, AppProperties props) {
		this.encoder = encoder;
		this.clock = clock;
		this.lifetime = Duration.ofMinutes(props.auth().accessTokenMinutes());
	}

	public AccessToken issue(AppUser user) {
		Instant now = clock.instant();
		Instant expiresAt = now.plus(lifetime);
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
			.issuer(ISSUER)
			.subject(String.valueOf(user.getId()))
			.issuedAt(now)
			.expiresAt(expiresAt)
			.claim(JwtConfig.CLAIM_ROLE, user.getRole().name())
			.claim(JwtConfig.CLAIM_PASSWORD_CHANGE, user.isMustChangePassword());
		if (user.getDoctorId() != null) {
			claims.claim(CLAIM_DOCTOR_ID, user.getDoctorId());
		}
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
		return new AccessToken(token, expiresAt);
	}

}
