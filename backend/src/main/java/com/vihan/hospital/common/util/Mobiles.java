package com.vihan.hospital.common.util;

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
 * Indian mobile numbers (DESIGN.md §19.2): stored as 10 digits starting with 6-9. People type them
 * in many ways ("+91 98765 43210", "098765-43210"), so everything is normalised before saving or
 * comparing — the per-mobile booking limit and booking lookup depend on it.
 */
public final class Mobiles {

	private Mobiles() {
	}

	/** @return the 10-digit number, or null if it isn't a valid Indian mobile number */
	public static String normalize(String input) {
		if (input == null) {
			return null;
		}
		String digits = input.replaceAll("[\\s\\-().]", "");
		if (digits.startsWith("+91")) {
			digits = digits.substring(3);
		}
		else if (digits.length() == 12 && digits.startsWith("91")) {
			digits = digits.substring(2);
		}
		else if (digits.length() == 11 && digits.startsWith("0")) {
			digits = digits.substring(1);
		}
		return digits.matches("[6-9]\\d{9}") ? digits : null;
	}

	/** Bean Validation: the value must normalise to a valid Indian mobile number (null/blank allowed). */
	@Documented
	@Constraint(validatedBy = IndianMobile.Validator.class)
	@Target({ ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface IndianMobile {

		String message() default "must be a 10-digit Indian mobile number";

		Class<?>[] groups() default {};

		Class<? extends Payload>[] payload() default {};

		class Validator implements ConstraintValidator<IndianMobile, String> {
			@Override
			public boolean isValid(String value, ConstraintValidatorContext context) {
				return value == null || value.isBlank() || normalize(value) != null;
			}
		}

	}

}
