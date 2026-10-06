package com.vihan.hospital.audit;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.vihan.hospital.security.CurrentUser;

/**
 * Writes audit entries in the caller's transaction, so an action that is rolled back is not
 * logged as done. (Failed logins commit on purpose: see AuthService.)
 */
@Service
public class AuditService {

	public record AuditLogDto(Long id, LocalDateTime createdAt, Long userId, String username, AuditAction action,
			String entityType, String entityId, String details, String ipAddress) {
	}

	private static final int MAX_DETAILS = 1000;
	private static final int MAX_PAGE_SIZE = 200;

	private final AuditLogRepository repository;
	private final Clock clock;

	public AuditService(AuditLogRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	/** Logs an action by the current user (or anonymous). */
	@Transactional(propagation = Propagation.MANDATORY)
	public void record(AuditAction action, String entityType, Object entityId, String details) {
		recordFor(CurrentUser.id().orElse(null), action, entityType, entityId, details);
	}

	/** Logs an action for a specific user, e.g. during login when nobody is authenticated yet. */
	@Transactional(propagation = Propagation.MANDATORY)
	public void recordFor(Long userId, AuditAction action, String entityType, Object entityId, String details) {
		String safeDetails = details != null && details.length() > MAX_DETAILS ? details.substring(0, MAX_DETAILS) : details;
		repository.save(new AuditLog(userId, action, entityType, entityId == null ? null : String.valueOf(entityId),
				safeDetails, clientIp(), LocalDateTime.now(clock)));
	}

	/** @param to inclusive end date */
	@Transactional(readOnly = true)
	public Page<AuditLogDto> search(Long userId, AuditAction action, LocalDate from, LocalDate to, int page, int size) {
		return repository.search(userId, action, from == null ? null : from.atStartOfDay(),
				to == null ? null : to.plusDays(1).atStartOfDay(),
				PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE)));
	}

	/** Behind Caddy, Spring's forwarded-header support (cloud profile) makes this the real client IP. */
	private static String clientIp() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
			HttpServletRequest request = attrs.getRequest();
			return request.getRemoteAddr();
		}
		return null;
	}

}
