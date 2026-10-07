package com.vihan.hospital.dashboard;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vihan.hospital.dashboard.DashboardDtos.DashboardSummary;

/** Dashboard (DESIGN.md §11.4). Doctors get their own figures only (inside the service). */
@RestController
@RequestMapping("/api/staff/dashboard")
public class DashboardController {

	private final DashboardService dashboard;

	public DashboardController(DashboardService dashboard) {
		this.dashboard = dashboard;
	}

	@GetMapping("/summary")
	public DashboardSummary summary(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(required = false) String branch) {
		return dashboard.summary(date, branch);
	}

	@GetMapping("/summary/export")
	@PreAuthorize("hasAnyRole('RECEPTIONIST', 'IT_ADMIN')")
	public ResponseEntity<byte[]> export(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(required = false) String branch, @RequestParam(defaultValue = "xlsx") String format) {
		return dashboard.export(date, branch, format);
	}

}
