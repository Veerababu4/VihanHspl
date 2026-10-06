package com.vihan.hospital.audit;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vihan.hospital.audit.AuditService.AuditLogDto;
import com.vihan.hospital.common.web.PageDto;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('IT_ADMIN')")
public class AdminAuditController {

	private final AuditService audit;

	public AdminAuditController(AuditService audit) {
		this.audit = audit;
	}

	@GetMapping
	public PageDto<AuditLogDto> search(@RequestParam(required = false) Long userId,
			@RequestParam(required = false) AuditAction action,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
		Page<AuditLogDto> result = audit.search(userId, action, from, to, page, size);
		return PageDto.from(result);
	}

}
