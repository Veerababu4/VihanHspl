package com.vihan.hospital.patient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.vihan.hospital.appointment.Appointment;
import com.vihan.hospital.common.util.Mobiles.IndianMobile;
import com.vihan.hospital.patient.Aadhaar.ValidAadhaar;
import com.vihan.hospital.visit.Visit;

/**
 * Patient API shapes (DESIGN.md §11.5). No response carries the full Aadhaar number: only
 * {@code aadhaarMasked} ({@code XXXX-XXXX-1234}). Nothing clinical is here (§12.3).
 */
public final class PatientDtos {

	private PatientDtos() {
	}

	private static final String BLOOD_GROUPS = "A\\+|A-|B\\+|B-|AB\\+|AB-|O\\+|O-";

	/**
	 * Registration (FR-PT1). Age or exact date of birth (§10.6). Everything after {@code consent} is
	 * optional, so the short check-in / walk-in form sends only the first fields.
	 *
	 * @param branch           registered branch code; defaults to the receptionist's home branch
	 * @param confirmDuplicate the user saw the "possibly the same person" warning and chose to go on
	 */
	public record RegisterPatientRequest(
			@NotBlank @Size(min = 2, max = 120) String fullName,
			@Min(0) @Max(120) Integer age,
			@Past LocalDate dateOfBirth,
			@NotBlank @Pattern(regexp = "[MFO]", message = "must be M, F or O") String gender,
			@NotBlank @IndianMobile String mobile,
			@Size(max = 10) String branch,
			@AssertTrue(message = "the patient must agree to the privacy notice") boolean consent,
			@Size(max = 20) @ValidAadhaar String aadhaar,
			@IndianMobile String altMobile,
			@Size(max = 400) String address,
			@Size(max = 60) String city,
			@Pattern(regexp = BLOOD_GROUPS, message = "must be like A+, O- or AB+") String bloodGroup,
			@Size(max = 120) String emergencyName,
			@IndianMobile String emergencyPhone,
			Boolean confirmDuplicate) {

		@AssertTrue(message = "enter the age or the date of birth")
		public boolean isAgeOrDateOfBirth() {
			return age != null || dateOfBirth != null;
		}
	}

	/**
	 * Edit (FR-PT5). {@code aadhaar} blank = keep the stored number (it is never sent to the screen,
	 * so it can't be sent back); {@code removeAadhaar} deletes it.
	 */
	public record UpdatePatientRequest(
			@NotBlank @Size(min = 2, max = 120) String fullName,
			@Min(0) @Max(120) Integer age,
			@Past LocalDate dateOfBirth,
			@NotBlank @Pattern(regexp = "[MFO]", message = "must be M, F or O") String gender,
			@NotBlank @IndianMobile String mobile,
			@Size(max = 20) @ValidAadhaar String aadhaar,
			Boolean removeAadhaar,
			@IndianMobile String altMobile,
			@Size(max = 400) String address,
			@Size(max = 60) String city,
			@Pattern(regexp = BLOOD_GROUPS, message = "must be like A+, O- or AB+") String bloodGroup,
			@Size(max = 120) String emergencyName,
			@IndianMobile String emergencyPhone,
			Boolean confirmDuplicate) {

		@AssertTrue(message = "enter the age or the date of birth")
		public boolean isAgeOrDateOfBirth() {
			return age != null || dateOfBirth != null;
		}
	}

	/** Aadhaar search and the duplicate check go in a POST body, never in a URL (proxy logs, history). */
	public record AadhaarSearchRequest(@NotBlank @Size(max = 20) @ValidAadhaar String aadhaar) {
	}

	public record DuplicateCheckRequest(@Size(max = 120) String name, @IndianMobile String mobile,
			@Size(max = 20) @ValidAadhaar String aadhaar, Long excludeId) {
	}

	/** @param aadhaarMatch the patient already registered with this Aadhaar (blocks saving) */
	public record DuplicateCheck(PatientSummary aadhaarMatch, List<PatientSummary> similar) {
	}

	public record PatientSummary(Long id, String uhid, String fullName, int age, LocalDate dateOfBirth,
			boolean dobEstimated, String gender, String mobile, String registeredBranchCode, String aadhaarMasked) {
	}

	/** A check-in suggestion and why it was suggested (DESIGN.md §19.5). */
	public record PatientMatch(PatientSummary patient, boolean sameMobile, boolean sameName, boolean ageClose) {
	}

	/** Profile (FR-PT4): demographics and upcoming appointments; the visit list is a separate call. */
	public record PatientDetail(Long id, String uhid, String fullName, int age, LocalDate dateOfBirth,
			boolean dobEstimated, String gender, String mobile, String altMobile, String aadhaarMasked, String address,
			String city, String bloodGroup, String emergencyName, String emergencyPhone, String registeredBranchCode,
			String registeredBranchCity, LocalDateTime registeredAt, List<UpcomingAppointment> upcoming) {
	}

	public record UpcomingAppointment(Long id, String bookingRef, LocalDate date, LocalDateTime startAt, Integer tokenNo,
			String doctorName, String branchCity, Appointment.Status status) {
	}

	/** Non-clinical visit list (§4: reception and IT Admin see this, never diagnosis or notes). */
	public record VisitRow(Long id, LocalDate date, String branchCity, String doctorName, String speciality,
			Visit.Type type, Visit.Status status) {
	}

}
