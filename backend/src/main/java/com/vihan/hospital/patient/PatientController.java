package com.vihan.hospital.patient;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vihan.hospital.patient.PatientDtos.AadhaarSearchRequest;
import com.vihan.hospital.patient.PatientDtos.DuplicateCheck;
import com.vihan.hospital.patient.PatientDtos.DuplicateCheckRequest;
import com.vihan.hospital.patient.PatientDtos.PatientDetail;
import com.vihan.hospital.patient.PatientDtos.PatientSummary;
import com.vihan.hospital.patient.PatientDtos.RegisterPatientRequest;
import com.vihan.hospital.patient.PatientDtos.UpdatePatientRequest;
import com.vihan.hospital.patient.PatientDtos.VisitRow;

/**
 * Patients (DESIGN.md §11.5, §4): reception and IT Admin register and edit; doctors may also search
 * and view (demographics and the visit list; clinical history is a separate doctor-only API, M8).
 * Aadhaar only ever travels in request bodies, never in a URL.
 */
@RestController
@RequestMapping("/api/staff/patients")
public class PatientController {

	private final PatientService patients;

	public PatientController(PatientService patients) {
		this.patients = patients;
	}

	@GetMapping
	public List<PatientSummary> search(@RequestParam(required = false) String uhid,
			@RequestParam(required = false) String mobile, @RequestParam(required = false) String name) {
		return patients.search(uhid, mobile, name);
	}

	@PostMapping("/search-aadhaar")
	public List<PatientSummary> searchByAadhaar(@Valid @RequestBody AadhaarSearchRequest req) {
		return patients.searchByAadhaar(req.aadhaar());
	}

	@PostMapping("/duplicates")
	@PreAuthorize("hasAnyRole('RECEPTIONIST', 'IT_ADMIN')")
	public DuplicateCheck duplicates(@Valid @RequestBody DuplicateCheckRequest req) {
		return patients.duplicates(req);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('RECEPTIONIST', 'IT_ADMIN')")
	public PatientSummary register(@Valid @RequestBody RegisterPatientRequest req) {
		return patients.register(req);
	}

	@GetMapping("/{id}")
	public PatientDetail view(@PathVariable Long id) {
		return patients.view(id);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('RECEPTIONIST', 'IT_ADMIN')")
	public PatientDetail update(@PathVariable Long id, @Valid @RequestBody UpdatePatientRequest req) {
		return patients.update(id, req);
	}

	@GetMapping("/{id}/visits")
	public List<VisitRow> visits(@PathVariable Long id) {
		return patients.visitList(id);
	}

}
