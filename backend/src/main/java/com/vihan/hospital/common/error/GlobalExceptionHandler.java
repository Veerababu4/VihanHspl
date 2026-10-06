package com.vihan.hospital.common.error;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Maps every exception to the single {@link ApiError} shape. Messages are safe for the browser:
 * unexpected errors are logged with full detail but the client only sees a generic message.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiError> handleApi(ApiException ex) {
		ResponseEntity<ApiError> response = respond(ex.getStatus(), ex.getCode(), ex.getMessage());
		if (ex instanceof ApiException.TooManyRequestsException tooMany) {
			return ResponseEntity.status(response.getStatusCode())
				.header(HttpHeaders.RETRY_AFTER, String.valueOf(tooMany.getRetryAfterSeconds()))
				.body(response.getBody());
		}
		return response;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex) {
		List<ApiError.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
				.map(f -> new ApiError.FieldError(f.getField(), f.getDefaultMessage()))
				.toList();
		return respond(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Please correct the highlighted fields", fields);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ApiError> handleParamValidation(HandlerMethodValidationException ex) {
		List<ApiError.FieldError> fields = ex.getParameterValidationResults().stream()
				.flatMap(r -> r.getResolvableErrors().stream()
						.map(e -> new ApiError.FieldError(r.getMethodParameter().getParameterName(), e.getDefaultMessage())))
				.toList();
		return respond(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Invalid request parameters", fields);
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
			MissingServletRequestParameterException.class })
	ResponseEntity<ApiError> handleBadRequest(Exception ex) {
		return respond(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Malformed request");
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex) {
		return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found");
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ApiError> handleMethod(HttpRequestMethodNotSupportedException ex) {
		return respond(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Method not allowed");
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	ResponseEntity<ApiError> handleMediaType(HttpMediaTypeNotSupportedException ex) {
		return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Unsupported content type");
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	ResponseEntity<ApiError> handleUploadSize(MaxUploadSizeExceededException ex) {
		return respond(HttpStatus.CONTENT_TOO_LARGE, "FILE_TOO_LARGE", "File is larger than 5 MB");
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
		return respond(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission for this action");
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ResponseEntity<ApiError> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
		return respond(HttpStatus.CONFLICT, "CONCURRENT_UPDATE", "This record was changed by someone else. Please reload and try again.");
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex) {
		log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
		return respond(HttpStatus.CONFLICT, "DATA_CONFLICT", "The request conflicts with existing data");
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> handleUnexpected(Exception ex) {
		log.error("Unhandled exception", ex);
		return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Something went wrong. Please try again.");
	}

	private static ResponseEntity<ApiError> respond(HttpStatus status, String code, String message) {
		return respond(status, code, message, List.of());
	}

	private static ResponseEntity<ApiError> respond(HttpStatus status, String code, String message,
			List<ApiError.FieldError> fields) {
		return ResponseEntity.status(status).body(ApiError.of(status.value(), code, message, fields));
	}

}
