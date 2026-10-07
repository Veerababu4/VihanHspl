package com.vihan.hospital.clinical;

import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vihan.hospital.clinical.ClinicalDtos.History;
import com.vihan.hospital.clinical.ClinicalDtos.MedicalProfileDto;
import com.vihan.hospital.clinical.ClinicalDtos.MedicalProfileRequest;
import com.vihan.hospital.clinical.ClinicalDtos.Queue;
import com.vihan.hospital.clinical.ClinicalDtos.SaveVisitRequest;
import com.vihan.hospital.clinical.ClinicalDtos.VisitDetail;

/**
 * Doctor-only clinical API (DESIGN.md §11.6). Two locks: the URL rule in SecurityConfig and the
 * annotation here, so moving this controller can't open it to other roles by mistake.
 */
@RestController
@RequestMapping("/api/clinical")
@PreAuthorize("hasRole('DOCTOR')")
public class ClinicalController {

	private final ClinicalService clinical;

	public ClinicalController(ClinicalService clinical) {
		this.clinical = clinical;
	}

	@GetMapping("/queue")
	public Queue queue(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return clinical.queue(date);
	}

	@GetMapping("/visits/{id}")
	public VisitDetail visit(@PathVariable Long id) {
		return clinical.visit(id);
	}

	/** Vitals, notes and the whole prescription in one save. */
	@PutMapping("/visits/{id}")
	public VisitDetail save(@PathVariable Long id, @Valid @RequestBody SaveVisitRequest req) {
		return clinical.save(id, req);
	}

	@PostMapping("/visits/{id}/complete")
	public VisitDetail complete(@PathVariable Long id) {
		return clinical.complete(id);
	}

	@GetMapping("/patients/{id}/history")
	public History history(@PathVariable Long id) {
		return clinical.history(id);
	}

	@GetMapping("/patients/{id}/medical-profile")
	public MedicalProfileDto medicalProfile(@PathVariable Long id) {
		return clinical.medicalProfile(id);
	}

	@PutMapping("/patients/{id}/medical-profile")
	public MedicalProfileDto updateMedicalProfile(@PathVariable Long id, @Valid @RequestBody MedicalProfileRequest req) {
		return clinical.updateMedicalProfile(id, req);
	}

}
