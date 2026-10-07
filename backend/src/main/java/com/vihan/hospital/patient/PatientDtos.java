package com.vihan.hospital.patient;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.vihan.hospital.common.util.Mobiles.IndianMobile;

public final class PatientDtos {

	private PatientDtos() {
	}

	/**
	 * Minimal registration used at check-in and for walk-ins (M6). Age or exact date of birth
	 * (DESIGN.md §10.6). Aadhaar, contact details and duplicate checks come with M7.
	 *
	 * @param branch registered branch code; defaults to the receptionist's home branch
	 */
	public record RegisterPatientRequest(
			@NotBlank @Size(min = 2, max = 120) String fullName,
			@Min(0) @Max(120) Integer age,
			@Past LocalDate dateOfBirth,
			@NotBlank @Pattern(regexp = "[MFO]", message = "must be M, F or O") String gender,
			@NotBlank @IndianMobile String mobile,
			@Size(max = 10) String branch,
			@AssertTrue(message = "the patient must agree to the privacy notice") boolean consent) {

		@AssertTrue(message = "enter the age or the date of birth")
		public boolean isAgeOrDateOfBirth() {
			return age != null || dateOfBirth != null;
		}
	}

	public record PatientSummary(Long id, String uhid, String fullName, int age, LocalDate dateOfBirth,
			boolean dobEstimated, String gender, String mobile, String registeredBranchCode) {
	}

	/** A check-in suggestion and why it was suggested (DESIGN.md §19.5). */
	public record PatientMatch(PatientSummary patient, boolean sameMobile, boolean sameName, boolean ageClose) {
	}

}
