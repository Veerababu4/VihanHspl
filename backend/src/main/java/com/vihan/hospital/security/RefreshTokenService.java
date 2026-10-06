package com.vihan.hospital.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vihan.hospital.config.AppProperties;

/**
 * Refresh tokens (DESIGN.md §12.2): random 256-bit values, stored hashed, rotated on every use.
 * A session lasts at most one shift (8 h) from login; refreshing does not extend it.
 * Presenting an already-used token means it was copied, so the whole session family is revoked.
 */
@Service
public class RefreshTokenService {

	private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final int REUSE_GRACE_SECONDS = 30;

	/** Outcome of presenting a refresh token. */
	public sealed interface Rotation {
		record Rotated(Long userId, String newToken, LocalDateTime expiresAt) implements Rotation {
		}

		/** Token unknown or expired. */
		record Invalid() implements Rotation {
		}

		/** Token was already used: possible theft. All of the user's sessions were revoked. */
		record Reused(Long userId) implements Rotation {
		}
	}

	public record Issued(String token, LocalDateTime expiresAt) {
	}

	private final RefreshTokenRepository tokens;
	private final Clock clock;
	private final int sessionHours;

	public RefreshTokenService(RefreshTokenRepository tokens, Clock clock, AppProperties props) {
		this.tokens = tokens;
		this.clock = clock;
		this.sessionHours = props.auth().refreshTokenHours();
	}

	@Transactional
	public Issued issue(Long userId) {
		return issue(userId, LocalDateTime.now(clock).plusHours(sessionHours));
	}

	@Transactional
	public Rotation rotate(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return new Rotation.Invalid();
		}
		RefreshToken current = tokens.findByTokenHash(hash(rawToken)).orElse(null);
		if (current == null || !current.getExpiresAt().isAfter(LocalDateTime.now(clock))) {
			return new Rotation.Invalid();
		}
		if (current.isRevoked()) {
			if (current.getUpdatedAt() != null
					&& current.getUpdatedAt().isAfter(LocalDateTime.now(clock).minusSeconds(REUSE_GRACE_SECONDS))) {
				// Two tabs refreshing at the same moment with the same cookie: not theft.
				return new Rotation.Invalid();
			}
			tokens.revokeAllForUser(current.getUserId());
			log.warn("Reused refresh token for user {}: all sessions revoked", current.getUserId());
			return new Rotation.Reused(current.getUserId());
		}
		current.revoke();
		Issued next = issue(current.getUserId(), current.getExpiresAt());   // same session end time
		return new Rotation.Rotated(current.getUserId(), next.token(), next.expiresAt());
	}

	@Transactional
	public void revoke(String rawToken) {
		if (rawToken != null && !rawToken.isBlank()) {
			tokens.findByTokenHash(hash(rawToken)).ifPresent(RefreshToken::revoke);
		}
	}

	@Transactional
	public void revokeAll(Long userId) {
		tokens.revokeAllForUser(userId);
	}

	/** Nightly clean-up of old rows (kept one day past expiry so reuse can still be detected). */
	@Scheduled(cron = "0 15 3 * * *", zone = "Asia/Kolkata")
	@Transactional
	public void purgeExpired() {
		int removed = tokens.deleteExpiredBefore(LocalDateTime.now(clock).minusDays(1));
		log.info("Purged {} expired refresh tokens", removed);
	}

	private Issued issue(Long userId, LocalDateTime expiresAt) {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		tokens.save(new RefreshToken(userId, hash(raw), expiresAt));
		return new Issued(raw, expiresAt);
	}

	static String hash(String raw) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

}
