package com.vihan.hospital.doctor;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.vihan.hospital.branch.Branch;
import com.vihan.hospital.storage.FileUrls;

public final class DoctorDtos {

	private DoctorDtos() {
	}

	public record SpecialityRef(Long id, String name) {
	}

	public record BranchRef(String code, String city) {
	}

	/** Card in the doctor directory (DESIGN.md FR-P2). */
	public record DoctorSummaryDto(Long id, String fullName, String gender, SpecialityRef speciality,
			String qualification, String designation, int experienceYears, String languages, String photoUrl,
			BigDecimal consultationFee, List<BranchRef> branches) {

		public static DoctorSummaryDto from(Doctor d, FileUrls urls) {
			return new DoctorSummaryDto(d.getId(), d.getFullName(), d.getGender(), specialityRef(d),
					d.getQualification(), d.getDesignation(), d.experienceYears(), d.getLanguages(),
					urls.toUrl(d.getPhotoUrl()), d.getConsultationFee(), branchRefs(d));
		}
	}

	/** Doctor profile page with weekly availability per branch (FR-P3). */
	public record DoctorDetailDto(Long id, String fullName, String gender, SpecialityRef speciality,
			String qualification, String designation, String registrationNo, int experienceYears, String languages,
			String bio, String photoUrl, BigDecimal consultationFee, List<BranchRef> branches,
			List<BranchAvailability> availability) {
	}

	public record BranchAvailability(String branchCode, String branchCity, List<DayAvailability> days) {
	}

	/** @param dayOfWeek ISO day, 1 = Monday … 7 = Sunday */
	public record DayAvailability(int dayOfWeek, List<Session> sessions) {
	}

	/** @param start 'HH:mm' */
	public record Session(String start, String end, int slotMinutes) {
	}

	/** IT Admin view: every editable field, including inactive doctors. */
	public record AdminDoctorDto(Long id, String fullName, String gender, Long specialityId, String specialityName,
			String qualification, String designation, String registrationNo, int practiceStartYear,
			int experienceYears, String languages, String bio, String photoUrl, BigDecimal consultationFee,
			boolean active, List<Long> branchIds) {

		public static AdminDoctorDto from(Doctor d, FileUrls urls) {
			return new AdminDoctorDto(d.getId(), d.getFullName(), d.getGender(), d.getSpeciality().getId(),
					d.getSpeciality().getName(), d.getQualification(), d.getDesignation(), d.getRegistrationNo(),
					d.getPracticeStartYear(), d.experienceYears(), d.getLanguages(), d.getBio(),
					urls.toUrl(d.getPhotoUrl()), d.getConsultationFee(), d.isActive(),
					sortedBranches(d).stream().map(Branch::getId).toList());
		}
	}

	public record DoctorRequest(
			@NotBlank @Size(max = 120) String fullName,
			@Pattern(regexp = "[MFO]", message = "must be M, F or O") String gender,
			@NotNull Long specialityId,
			@NotBlank @Size(max = 300) String qualification,
			@Size(max = 120) String designation,
			@Size(max = 40) String registrationNo,
			@NotNull @Min(1950) @Max(2100) Integer practiceStartYear,
			@Size(max = 200) String languages,
			@Size(max = 2000) String bio,
			@DecimalMin("0") @Digits(integer = 6, fraction = 2) BigDecimal consultationFee,
			@NotEmpty(message = "select at least one branch") Set<Long> branchIds,
			@NotNull Boolean active) {
	}

	static SpecialityRef specialityRef(Doctor d) {
		return new SpecialityRef(d.getSpeciality().getId(), d.getSpeciality().getName());
	}

	/** Branches in display order (MLG, HZN, KDD = id order). */
	static List<Branch> sortedBranches(Doctor d) {
		return d.getBranches().stream().sorted(Comparator.comparing(Branch::getId)).toList();
	}

	static List<BranchRef> branchRefs(Doctor d) {
		return sortedBranches(d).stream()
			.filter(Branch::isActive)
			.map(b -> new BranchRef(b.getCode(), b.getCity()))
			.toList();
	}

}
