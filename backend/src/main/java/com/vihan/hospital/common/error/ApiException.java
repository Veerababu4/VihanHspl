package com.vihan.hospital.common.error;

import org.springframework.http.HttpStatus;

/**
 * Base for expected business errors. Services throw a subclass; {@link GlobalExceptionHandler}
 * turns it into an {@link ApiError} with the given status, code and message.
 */
public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	public ApiException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	/** 404 — e.g. {@code new NotFoundException("Doctor", id)}. */
	public static class NotFoundException extends ApiException {
		public NotFoundException(String entity, Object id) {
			super(HttpStatus.NOT_FOUND, "NOT_FOUND", entity + " not found: " + id);
		}
	}

	/** 409 — state conflicts such as a slot already booked or an invalid status change. */
	public static class ConflictException extends ApiException {
		public ConflictException(String code, String message) {
			super(HttpStatus.CONFLICT, code, message);
		}
	}

	/** 400 — business-rule violations that aren't simple field validation. */
	public static class BadRequestException extends ApiException {
		public BadRequestException(String code, String message) {
			super(HttpStatus.BAD_REQUEST, code, message);
		}
	}

}
