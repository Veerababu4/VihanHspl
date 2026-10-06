package com.vihan.hospital.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.vihan.hospital.user.Role;

/** Reads the logged-in staff member from the access token. Empty for public requests. */
public final class CurrentUser {

	private CurrentUser() {
	}

	public static Optional<Long> id() {
		return jwt().map(t -> Long.valueOf(t.getToken().getSubject()));
	}

	public static Optional<Role> role() {
		return jwt().map(t -> Role.valueOf(t.getToken().getClaimAsString(JwtConfig.CLAIM_ROLE)));
	}

	/** The doctor profile linked to a DOCTOR login (used for "own data only" checks from M4 on). */
	public static Optional<Long> doctorId() {
		return jwt().map(t -> t.getToken().getClaim(TokenService.CLAIM_DOCTOR_ID))
			.map(v -> ((Number) v).longValue());
	}

	private static Optional<JwtAuthenticationToken> jwt() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return auth instanceof JwtAuthenticationToken token ? Optional.of(token) : Optional.empty();
	}

}
