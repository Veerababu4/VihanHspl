package com.vihan.hospital.common.error;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * The single error shape returned by every endpoint (DESIGN.md §11).
 *
 * @param code        stable machine-readable code the frontend can switch on, e.g. {@code SLOT_TAKEN}
 * @param message     safe, human-readable text; never contains internal details
 * @param fieldErrors per-field validation problems, empty when not a validation error
 */
public record ApiError(OffsetDateTime timestamp, int status, String code, String message, List<FieldError> fieldErrors) {

	public record FieldError(String field, String message) {
	}

	public static ApiError of(int status, String code, String message) {
		return new ApiError(OffsetDateTime.now(), status, code, message, List.of());
	}

	public static ApiError of(int status, String code, String message, List<FieldError> fieldErrors) {
		return new ApiError(OffsetDateTime.now(), status, code, message, fieldErrors);
	}

}
