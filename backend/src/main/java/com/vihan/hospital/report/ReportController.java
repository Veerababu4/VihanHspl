package com.vihan.hospital.report;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/staff/reports/{type}?from=&to=&branch=&period=day|month&format=json|csv|xlsx}
 * (DESIGN.md §11.4). Exports are written to the audit log.
 */
@RestController
@RequestMapping("/api/staff/reports")
@PreAuthorize("hasAnyRole('RECEPTIONIST', 'IT_ADMIN')")
public class ReportController {

	private final ReportService reports;

	public ReportController(ReportService reports) {
		this.reports = reports;
	}

	@GetMapping("/{type}")
	public ResponseEntity<?> report(@PathVariable String type,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) String branch, @RequestParam(required = false) String period,
			@RequestParam(defaultValue = "json") String format) {
		if ("json".equalsIgnoreCase(format)) {
			return ResponseEntity.ok(reports.run(type, from, to, branch, period));
		}
		return reports.export(type, from, to, branch, period, format);
	}

}
