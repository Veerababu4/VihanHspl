package com.vihan.hospital.patient;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vihan.hospital.patient.PatientDtos.PatientSummary;
import com.vihan.hospital.patient.PatientDtos.RegisterPatientRequest;

/** Patient registration and lookup by mobile (DESIGN.md §11.5). Full search, profile and editing arrive in M7. */
@RestController
@RequestMapping("/api/staff/patients")
@PreAuthorize("hasAnyRole('RECEPTIONIST', 'IT_ADMIN')")
public class PatientController {

	private final PatientService patients;

	public PatientController(PatientService patients) {
		this.patients = patients;
	}

	@GetMapping
	public List<PatientSummary> search(@RequestParam String mobile) {
		return patients.byMobile(mobile);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PatientSummary register(@Valid @RequestBody RegisterPatientRequest req) {
		return patients.register(req);
	}

}
