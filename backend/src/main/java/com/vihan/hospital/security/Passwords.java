package com.vihan.hospital.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import com.vihan.hospital.common.error.ApiException.BadRequestException;

/** Password rules (DESIGN.md §12.2) and temporary-password generation. */
public final class Passwords {

	private static final SecureRandom RANDOM = new SecureRandom();
	// No look-alike characters (0/O, 1/l/I) so a temporary password can be read out over the phone.
	private static final String LETTERS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
	private static final String DIGITS = "23456789";
	private static final int TEMP_LENGTH = 12;
	private static final int BCRYPT_MAX_BYTES = 72;   // BCrypt ignores anything longer

	private Passwords() {
	}

	/** At least 8 characters with letters and digits. */
	public static void checkPolicy(String password) {
		if (password == null || password.length() < 8) {
			throw new BadRequestException("WEAK_PASSWORD", "Password must be at least 8 characters");
		}
		if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
			throw new BadRequestException("WEAK_PASSWORD", "Password is too long");
		}
		if (password.chars().noneMatch(Character::isLetter) || password.chars().noneMatch(Character::isDigit)) {
			throw new BadRequestException("WEAK_PASSWORD", "Password must contain both letters and digits");
		}
	}

	public static String temporary() {
		StringBuilder sb = new StringBuilder(TEMP_LENGTH);
		String all = LETTERS + DIGITS;
		sb.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
		sb.append(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
		while (sb.length() < TEMP_LENGTH) {
			sb.append(all.charAt(RANDOM.nextInt(all.length())));
		}
		// Shuffle so the guaranteed digit/letter aren't always first.
		for (int i = sb.length() - 1; i > 0; i--) {
			int j = RANDOM.nextInt(i + 1);
			char c = sb.charAt(i);
			sb.setCharAt(i, sb.charAt(j));
			sb.setCharAt(j, c);
		}
		return sb.toString();
	}

}
