package com.vihan.hospital.patient;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vihan.hospital.audit.AuditAction;
import com.vihan.hospital.audit.AuditService;
import com.vihan.hospital.branch.Branch;
import com.vihan.hospital.branch.BranchLookup;
import com.vihan.hospital.common.error.ApiException.BadRequestException;
import com.vihan.hospital.common.error.ApiException.NotFoundException;
import com.vihan.hospital.common.util.Mobiles;
import com.vihan.hospital.patient.PatientDtos.PatientMatch;
import com.vihan.hospital.patient.PatientDtos.PatientSummary;
import com.vihan.hospital.patient.PatientDtos.RegisterPatientRequest;
import com.vihan.hospital.security.CurrentUser;
import com.vihan.hospital.user.AppUser;
import com.vihan.hospital.user.AppUserRepository;

/** Patient registration with a network-wide UHID (FR-PT1). M6: the minimal form used at check-in. */
@Service
public class PatientService {

	/** "Same age ± 2" in the check-in suggestions (DESIGN.md §19.5). */
	private static final int AGE_TOLERANCE = 2;

	private final PatientRepository patients;
	private final BranchLookup branches;
	private final AppUserRepository users;
	private final AuditService audit;
	private final Clock clock;

	public PatientService(PatientRepository patients, BranchLookup branches, AppUserRepository users, AuditService audit,
			Clock clock) {
		this.patients = patients;
		this.branches = branches;
		this.users = users;
		this.audit = audit;
		this.clock = clock;
	}

	@Transactional
	public PatientSummary register(RegisterPatientRequest req) {
		LocalDate today = LocalDate.now(clock);
		boolean estimated = req.dateOfBirth() == null;
		LocalDate dob = estimated ? estimatedDob(req.age(), today) : req.dateOfBirth();
		Long branchId = registeredBranch(req.branch());
		String uhid = "VMH-%02d-%06d".formatted(today.getYear() % 100, patients.nextUhidNumber());
		Patient p = patients.save(new Patient(uhid, req.fullName().strip().replaceAll("\\s+", " "), dob, estimated,
				req.gender(), Mobiles.normalize(req.mobile()), branchId, LocalDateTime.now(clock)));
		audit.record(AuditAction.PATIENT_CREATED, "PATIENT", p.getId(), uhid);
		return summary(p, branches.byId(), today);
	}

	/** Same mobile, or the same name with any mobile; same-mobile first. */
	@Transactional(readOnly = true)
	public List<PatientMatch> candidates(String name, String mobile, Integer age) {
		LocalDate today = LocalDate.now(clock);
		Map<Long, Branch> branchMap = branches.byId();
		return patients.findCandidates(mobile, name.strip()).stream()
			.limit(20)
			.map(p -> new PatientMatch(summary(p, branchMap, today), p.getMobile().equals(mobile),
					p.getFullName().equalsIgnoreCase(name.strip()),
					age != null && Math.abs(p.ageOn(today) - age) <= AGE_TOLERANCE))
			.toList();
	}

	/** Walk-in desk: "is this patient already registered?" by mobile. M7 adds UHID / name / Aadhaar search. */
	@Transactional(readOnly = true)
	public List<PatientSummary> byMobile(String mobile) {
		String normalized = Mobiles.normalize(mobile);
		if (normalized == null) {
			throw new BadRequestException("INVALID_MOBILE", "Enter a 10-digit mobile number");
		}
		LocalDate today = LocalDate.now(clock);
		Map<Long, Branch> branchMap = branches.byId();
		return patients.findTop20ByActiveTrueAndMobileOrderByFullName(normalized).stream()
			.map(p -> summary(p, branchMap, today))
			.toList();
	}

	@Transactional(readOnly = true)
	public Patient activePatient(Long id) {
		return patients.findById(id).filter(Patient::isActive).orElseThrow(() -> new NotFoundException("Patient", id));
	}

	public PatientSummary summary(Patient p, Map<Long, Branch> branchMap, LocalDate today) {
		Branch b = branchMap.get(p.getRegisteredBranchId());
		return new PatientSummary(p.getId(), p.getUhid(), p.getFullName(), p.ageOn(today), p.getDateOfBirth(),
				p.isDobEstimated(), p.getGender(), p.getMobile(), b == null ? null : b.getCode());
	}

	/**
	 * Only an age was given: 1 July of the birth year (DESIGN.md §10.6). Before 1 July the patient
	 * has not had this year's birthday yet, so the year is one earlier — otherwise a patient
	 * registered as 30 in March would show as 29.
	 */
	static LocalDate estimatedDob(int age, LocalDate today) {
		LocalDate dob = LocalDate.of(today.getYear() - age, 7, 1);
		return dob.isAfter(today.minusYears(age)) ? dob.minusYears(1) : dob;
	}

	private Long registeredBranch(String code) {
		Long id = branches.idOf(code);
		if (id != null) {
			return id;
		}
		return CurrentUser.id().flatMap(users::findById).map(AppUser::getHomeBranchId)
			.orElseThrow(() -> new BadRequestException("BRANCH_REQUIRED", "Choose the branch where the patient is registered"));
	}

}
