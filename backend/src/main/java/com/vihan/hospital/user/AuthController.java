package com.vihan.hospital.user;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vihan.hospital.common.error.ApiError;
import com.vihan.hospital.common.error.ApiException;
import com.vihan.hospital.config.AppProperties;
import com.vihan.hospital.security.CurrentUser;
import com.vihan.hospital.security.RefreshTokenService.Issued;
import com.vihan.hospital.user.AuthService.SessionResult;
import com.vihan.hospital.user.UserDtos.ChangePasswordRequest;
import com.vihan.hospital.user.UserDtos.LoginRequest;
import com.vihan.hospital.user.UserDtos.MeDto;
import com.vihan.hospital.user.UserDtos.SessionDto;

/**
 * /api/auth (DESIGN.md §11.2). The access token goes in the JSON body (kept in memory by the
 * browser); the refresh token only ever travels in an httpOnly, SameSite=Strict cookie scoped to
 * /api/auth, so page scripts can't read it.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	static final String REFRESH_COOKIE = "vihan_refresh";
	private static final String COOKIE_PATH = "/api/auth";

	private final AuthService auth;
	private final Clock clock;
	private final boolean secureCookie;

	public AuthController(AuthService auth, Clock clock, AppProperties props) {
		this.auth = auth;
		this.clock = clock;
		this.secureCookie = props.auth().cookieSecure();
	}

	@PostMapping("/login")
	public ResponseEntity<SessionDto> login(@Valid @RequestBody LoginRequest req) {
		return withCookie(auth.login(req.username(), req.password()));
	}

	@PostMapping("/refresh")
	public ResponseEntity<?> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String token) {
		if (token == null || token.isBlank()) {
			// Nobody logged in on this browser: a normal answer, not an error (keeps the console clean).
			return ResponseEntity.noContent().build();
		}
		try {
			return withCookie(auth.refresh(token));
		}
		catch (ApiException e) {
			// Clear a dead cookie so the browser stops sending it.
			return ResponseEntity.status(e.getStatus())
				.header(HttpHeaders.SET_COOKIE, clearCookie().toString())
				.body(ApiError.of(e.getStatus().value(), e.getCode(), e.getMessage()));
		}
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String token) {
		auth.logout(CurrentUser.id().orElse(null), token);
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clearCookie().toString()).build();
	}

	@PostMapping("/change-password")
	public ResponseEntity<SessionDto> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
		Long userId = CurrentUser.id().orElseThrow();
		return withCookie(auth.changePassword(userId, req.currentPassword(), req.newPassword()));
	}

	@GetMapping("/me")
	public MeDto me() {
		return auth.me(CurrentUser.id().orElseThrow());
	}

	private ResponseEntity<SessionDto> withCookie(SessionResult result) {
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString())
			.body(result.session());
	}

	private ResponseCookie refreshCookie(Issued token) {
		Duration maxAge = Duration.between(LocalDateTime.now(clock), token.expiresAt());
		return baseCookie(token.token()).maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge).build();
	}

	private ResponseCookie clearCookie() {
		return baseCookie("").maxAge(Duration.ZERO).build();
	}

	private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
		return ResponseCookie.from(REFRESH_COOKIE, value)
			.httpOnly(true)
			.secure(secureCookie)
			.sameSite("Strict")
			.path(COOKIE_PATH);
	}

}
