package com.vihan.hospital.appointment;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Booking references {@code VMH-YYMMDD-XXXX} (DESIGN.md §10.1). The 4 random characters come from
 * 32 symbols without look-alikes (0/O, 1/I), so a reference is easy to read over the phone and hard
 * to guess (about a million per day; lookup also needs the mobile number and is rate-limited).
 */
public final class BookingRefs {

	static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyMMdd");
	private static final SecureRandom RANDOM = new SecureRandom();

	private BookingRefs() {
	}

	/** @param day the day the booking is made */
	public static String generate(LocalDate day) {
		StringBuilder sb = new StringBuilder("VMH-").append(DATE.format(day)).append('-');
		for (int i = 0; i < 4; i++) {
			sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
		}
		return sb.toString();
	}

	/** Cleans up a typed reference: trims, upper-cases, and accepts it without the dashes. */
	public static String normalize(String input) {
		if (input == null) {
			return "";
		}
		String s = input.strip().toUpperCase(Locale.ROOT).replace(" ", "");
		if (s.matches("VMH\\d{6}[A-Z0-9]{4}")) {
			s = s.substring(0, 3) + "-" + s.substring(3, 9) + "-" + s.substring(9);
		}
		return s;
	}

}
