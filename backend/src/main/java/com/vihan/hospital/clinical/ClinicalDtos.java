package com.vihan.hospital.clinical;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.vihan.hospital.appointment.Appointment;
import com.vihan.hospital.visit.Visit;

/**
 * Doctor-only clinical API shapes (DESIGN.md §11.6, §12.3). Kept apart from the patient /
 * reception DTOs on purpose, so a clinical field can't reach a non-doctor endpoint by accident.
 */
public final class ClinicalDtos {

	private ClinicalDtos() {
	}

	// ---- queue (FR-C1) ----

	/** @param hasAllergies the medical profile lists allergies: shown before the doctor prescribes */
	public record QueueEntry(Long appointmentId, Long visitId, Integer tokenNo, LocalDateTime startAt,
			LocalDateTime checkedInAt, Appointment.Channel channel, Long patientId, String uhid, String patientName,
			Integer age, String gender, String reason, String branchCity, Visit.Status visitStatus, boolean hasAllergies) {
	}

	public record Queue(LocalDate date, List<QueueEntry> waiting, List<QueueEntry> done) {
	}

	// ---- one visit (FR-C2, FR-C3, FR-C5) ----

	public record PatientHeader(Long id, String uhid, String fullName, int age, String gender, String mobile) {
	}

	public record DoctorHeader(Long id, String fullName, String speciality, String qualification, String registrationNo) {
	}

	public record BranchHeader(String code, String name, String city, String address, String phone) {
	}

	public record RxItem(String medicineName, String dosage, String frequency, String duration, String instructions) {
	}

	/**
	 * Everything the visit screen and the printed prescription need.
	 *
	 * @param canEdit the logged-in doctor treats this visit and it is still open
	 */
	public record VisitDetail(Long id, LocalDate date, Visit.Type type, Visit.Status status, boolean canEdit,
			PatientHeader patient, String allergies, DoctorHeader doctor, BranchHeader branch, Integer tokenNo,
			String bookingReason, Visit.ClinicalNote clinical, List<RxItem> prescription) {
	}

	/** One medicine line; only the name is required. */
	public record RxItemRequest(
			@NotBlank @Size(max = 200) String medicineName,
			@Size(max = 60) String dosage,
			@Size(max = 60) String frequency,
			@Size(max = 60) String duration,
			@Size(max = 300) String instructions) {
	}

	/**
	 * Save the visit and its prescription together (one transaction, so they can't get out of step).
	 * Ranges only catch typing mistakes (e.g. 1200 for 120); every field may be left empty.
	 */
	public record SaveVisitRequest(
			@Min(50) @Max(300) Integer bpSystolic,
			@Min(30) @Max(200) Integer bpDiastolic,
			@Min(20) @Max(250) Integer pulse,
			@DecimalMin("90.0") @DecimalMax("110.0") BigDecimal temperatureF,
			@Min(50) @Max(100) Integer spo2,
			@DecimalMin("0.5") @DecimalMax("300.0") BigDecimal weightKg,
			@DecimalMin("30.0") @DecimalMax("250.0") BigDecimal heightCm,
			@Size(max = 1000) String chiefComplaint,
			@Size(max = 1000) String diagnosis,
			@Size(max = 10000) String clinicalNotes,
			@Size(max = 2000) String advice,
			LocalDate followUpDate,
			@NotNull @Size(max = 30) List<@Valid RxItemRequest> prescription) {
	}

	// ---- history and medical profile (FR-C4) ----

	public record MedicalProfileDto(String allergies, String chronicConditions, String pastSurgeries,
			String currentMedications, String familyHistory, LocalDateTime updatedAt) {
	}

	public record MedicalProfileRequest(
			@Size(max = 1000) String allergies,
			@Size(max = 1000) String chronicConditions,
			@Size(max = 1000) String pastSurgeries,
			@Size(max = 1000) String currentMedications,
			@Size(max = 1000) String familyHistory) {
	}

	public record HistoryVisit(Long id, LocalDate date, Visit.Type type, Visit.Status status, String doctorName,
			String speciality, String branchCity, Visit.ClinicalNote clinical, List<RxItem> prescription) {
	}

	/** Across all doctors and branches (DESIGN.md §17 #11), newest first. */
	public record History(PatientHeader patient, MedicalProfileDto medicalProfile, List<HistoryVisit> visits) {
	}

}
