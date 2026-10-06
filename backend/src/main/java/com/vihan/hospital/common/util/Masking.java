package com.vihan.hospital.common.util;

/** Masks personal data for logs, exports and display (DESIGN.md §10.7, §12.4). */
public final class Masking {

	private Masking() {
	}

	/** {@code 9876543221 -> 98XXXXXX21}. Anything that isn't a 10-digit number is fully masked. */
	public static String mobile(String mobile) {
		if (mobile == null || !mobile.matches("\\d{10}")) {
			return "XXXXXXXXXX";
		}
		return mobile.substring(0, 2) + "XXXXXX" + mobile.substring(8);
	}

	/** {@code 1234 -> XXXX-XXXX-1234}; null when the patient gave no Aadhaar. */
	public static String aadhaarFromLast4(String last4) {
		return last4 == null ? null : "XXXX-XXXX-" + last4;
	}

}
