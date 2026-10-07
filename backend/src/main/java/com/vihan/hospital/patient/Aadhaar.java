package com.vihan.hospital.patient;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * Aadhaar number checks (DESIGN.md §10.7): 12 digits, not starting with 0 or 1, valid Verhoeff
 * check digit. Spaces and dashes are allowed when typed ("2345 6789 0124").
 */
public final class Aadhaar {

	// Verhoeff tables: multiplication in the dihedral group D5, the position permutation, the inverse.
	private static final int[][] D = {
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, { 1, 2, 3, 4, 0, 6, 7, 8, 9, 5 }, { 2, 3, 4, 0, 1, 7, 8, 9, 5, 6 },
			{ 3, 4, 0, 1, 2, 8, 9, 5, 6, 7 }, { 4, 0, 1, 2, 3, 9, 5, 6, 7, 8 }, { 5, 9, 8, 7, 6, 0, 4, 3, 2, 1 },
			{ 6, 5, 9, 8, 7, 1, 0, 4, 3, 2 }, { 7, 6, 5, 9, 8, 2, 1, 0, 4, 3 }, { 8, 7, 6, 5, 9, 3, 2, 1, 0, 4 },
			{ 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 } };
	private static final int[][] P = {
			{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, { 1, 5, 7, 6, 2, 8, 3, 0, 9, 4 }, { 5, 8, 0, 3, 7, 9, 6, 1, 4, 2 },
			{ 8, 9, 1, 6, 0, 4, 3, 5, 2, 7 }, { 9, 4, 5, 3, 1, 2, 6, 8, 7, 0 }, { 4, 2, 8, 6, 5, 7, 3, 9, 0, 1 },
			{ 2, 7, 9, 3, 8, 0, 6, 4, 1, 5 }, { 7, 0, 4, 6, 9, 1, 3, 2, 5, 8 } };
	private static final int[] INV = { 0, 4, 3, 2, 1, 5, 6, 7, 8, 9 };

	private Aadhaar() {
	}

	/** @return the 12 digits, or null if this is not a valid Aadhaar number */
	public static String normalize(String input) {
		if (input == null) {
			return null;
		}
		String digits = input.replaceAll("[\\s-]", "");
		return digits.matches("[2-9]\\d{11}") && verhoeffValid(digits) ? digits : null;
	}

	static boolean verhoeffValid(String digits) {
		int c = 0;
		for (int i = 0; i < digits.length(); i++) {
			int d = digits.charAt(digits.length() - 1 - i) - '0';
			c = D[c][P[i % 8][d]];
		}
		return c == 0;
	}

	/** The check digit to append to {@code digits} (used to make fictional sample numbers). */
	static int verhoeffCheckDigit(String digits) {
		int c = 0;
		for (int i = 0; i < digits.length(); i++) {
			int d = digits.charAt(digits.length() - 1 - i) - '0';
			c = D[c][P[(i + 1) % 8][d]];
		}
		return INV[c];
	}

	/**
	 * Bean Validation: a valid Aadhaar number (null/blank allowed: it is optional). The message never
	 * repeats the value, so a rejected number doesn't end up in an error response or log.
	 */
	@Documented
	@Constraint(validatedBy = ValidAadhaar.Validator.class)
	@Target({ ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ValidAadhaar {

		String message() default "is not a valid Aadhaar number";

		Class<?>[] groups() default {};

		Class<? extends Payload>[] payload() default {};

		class Validator implements ConstraintValidator<ValidAadhaar, String> {
			@Override
			public boolean isValid(String value, ConstraintValidatorContext context) {
				return value == null || value.isBlank() || normalize(value) != null;
			}
		}

	}

}
