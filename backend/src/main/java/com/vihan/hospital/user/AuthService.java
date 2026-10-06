package com.vihan.hospital.user;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vihan.hospital.audit.AuditAction;
import com.vihan.hospital.audit.AuditService;
import com.vihan.hospital.common.error.ApiException;
import com.vihan.hospital.common.error.ApiException.BadRequestException;
import com.vihan.hospital.common.error.ApiException.NotFoundException;
import com.vihan.hospital.config.AppProperties;
import com.vihan.hospital.security.Passwords;
import com.vihan.hospital.security.RefreshTokenService;
import com.vihan.hospital.security.RefreshTokenService.Issued;
import com.vihan.hospital.security.RefreshTokenService.Rotation;
import com.vihan.hospital.security.TokenService;
import com.vihan.hospital.user.UserDtos.MeDto;
import com.vihan.hospital.user.UserDtos.SessionDto;

/**
 * Staff login, token refresh, logout and password change (DESIGN.md §5.3, §12.2).
 * Methods that record a failure (failed attempt, lockout, audit) and then throw use
 * {@code noRollbackFor} so the failure itself is still saved.
 */
@Service
public class AuthService {

	/** What the controller needs: the JSON body plus the refresh token for the cookie. */
	public record SessionResult(SessionDto session, Issued refreshToken) {
	}

	private static final String INVALID_LOGIN = "Invalid username or password";

	private final AppUserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;
	private final RefreshTokenService refreshTokens;
	private final AuditService audit;
	private final Clock clock;
	private final AppProperties.Auth settings;
	/** Checked when the username doesn't exist, so the response takes as long as a real check. */
	private final String dummyHash;

	public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, TokenService tokenService,
			RefreshTokenService refreshTokens, AuditService audit, Clock clock, AppProperties props) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.refreshTokens = refreshTokens;
		this.audit = audit;
		this.clock = clock;
		this.settings = props.auth();
		this.dummyHash = passwordEncoder.encode("timing-equaliser-not-a-real-password-1");
	}

	@Transactional(noRollbackFor = ApiException.class)
	public SessionResult login(String username, String password) {
		String name = username.strip().toLowerCase(Locale.ROOT);
		LocalDateTime now = LocalDateTime.now(clock);
		AppUser user = users.findByUsername(name).orElse(null);

		if (user == null) {
			passwordEncoder.matches(password, dummyHash);
			audit.recordFor(null, AuditAction.LOGIN_FAILED, "USER", null, "unknown username: " + name);
			throw invalidLogin();
		}
		if (user.isLocked(now)) {
			audit.recordFor(user.getId(), AuditAction.LOGIN_BLOCKED, "USER", user.getId(), "account locked");
			throw locked(user.getLockedUntil(), now);
		}
		boolean passwordOk = passwordEncoder.matches(password, user.getPasswordHash());
		if (!user.isActive()) {
			// Same answer as a wrong password: don't reveal that the account exists but is disabled.
			audit.recordFor(user.getId(), AuditAction.LOGIN_FAILED, "USER", user.getId(), "account inactive");
			throw invalidLogin();
		}
		if (!passwordOk) {
			boolean nowLocked = user.recordFailedLogin(now, settings.maxFailedAttempts(), settings.lockMinutes());
			if (nowLocked) {
				audit.recordFor(user.getId(), AuditAction.LOGIN_LOCKED, "USER", user.getId(),
						"locked for " + settings.lockMinutes() + " min after " + settings.maxFailedAttempts() + " failures");
				throw locked(user.getLockedUntil(), now);
			}
			audit.recordFor(user.getId(), AuditAction.LOGIN_FAILED, "USER", user.getId(),
					"wrong password, attempt " + user.getFailedAttempts());
			throw invalidLogin();
		}

		user.recordSuccessfulLogin(now);
		audit.recordFor(user.getId(), AuditAction.LOGIN_SUCCESS, "USER", user.getId(), null);
		return newSession(user, refreshTokens.issue(user.getId()));
	}

	@Transactional(noRollbackFor = ApiException.class)
	public SessionResult refresh(String rawRefreshToken) {
		Rotation rotation = refreshTokens.rotate(rawRefreshToken);
		return switch (rotation) {
			case Rotation.Rotated r -> {
				AppUser user = users.findById(r.userId()).orElse(null);
				if (user == null || !user.isActive()) {
					refreshTokens.revokeAll(r.userId());
					throw sessionExpired();
				}
				yield newSession(user, new Issued(r.newToken(), r.expiresAt()));
			}
			case Rotation.Reused r -> {
				audit.recordFor(r.userId(), AuditAction.REFRESH_TOKEN_REUSE, "USER", r.userId(),
						"a used refresh token was presented again; all sessions revoked");
				throw sessionExpired();
			}
			case Rotation.Invalid i -> throw sessionExpired();
		};
	}

	@Transactional
	public void logout(Long userId, String rawRefreshToken) {
		refreshTokens.revoke(rawRefreshToken);
		if (userId != null) {
			audit.recordFor(userId, AuditAction.LOGOUT, "USER", userId, null);
		}
	}

	/** Changes the password and starts a fresh session; other devices are logged out. */
	@Transactional
	public SessionResult changePassword(Long userId, String currentPassword, String newPassword) {
		AppUser user = users.findById(userId).orElseThrow(() -> new NotFoundException("User", userId));
		if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
			throw new BadRequestException("WRONG_PASSWORD", "Current password is not correct");
		}
		Passwords.checkPolicy(newPassword);
		if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
			throw new BadRequestException("SAME_PASSWORD", "New password must be different from the current one");
		}
		user.setPassword(passwordEncoder.encode(newPassword), false);
		refreshTokens.revokeAll(userId);
		audit.recordFor(userId, AuditAction.PASSWORD_CHANGED, "USER", userId, null);
		return newSession(user, refreshTokens.issue(userId));
	}

	@Transactional(readOnly = true)
	public MeDto me(Long userId) {
		return users.findById(userId).map(MeDto::from).orElseThrow(() -> new NotFoundException("User", userId));
	}

	private SessionResult newSession(AppUser user, Issued refresh) {
		TokenService.AccessToken access = tokenService.issue(user);
		return new SessionResult(new SessionDto(access.value(), access.expiresAt(), MeDto.from(user)), refresh);
	}

	private static ApiException invalidLogin() {
		return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", INVALID_LOGIN);
	}

	private static ApiException sessionExpired() {
		return new ApiException(HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED", "Your session has ended. Please log in again.");
	}

	private static ApiException locked(LocalDateTime until, LocalDateTime now) {
		long minutes = Math.max(1, Duration.between(now, until).toMinutes() + 1);
		return new ApiException(HttpStatus.LOCKED, "ACCOUNT_LOCKED",
				"Too many failed attempts. Try again in " + minutes + " minutes or ask IT Admin to reset your password.");
	}

}
