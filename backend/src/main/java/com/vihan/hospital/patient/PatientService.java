package com.vihan.hospital.patient;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vihan.hospital.appointment.AppointmentRepository;
import com.vihan.hospital.audit.AuditAction;
import com.vihan.hospital.audit.AuditService;
import com.vihan.hospital.branch.Branch;
import com.vihan.hospital.branch.BranchLookup;
import com.vihan.hospital.common.error.ApiException.BadRequestException;
import com.vihan.hospital.common.error.ApiException.ConflictException;
import com.vihan.hospital.common.error.ApiException.NotFoundException;
import com.vihan.hospital.common.util.Masking;
import com.vihan.hospital.common.util.Mobiles;
import com.vihan.hospital.doctor.Doctor;
import com.vihan.hospital.doctor.DoctorRepository;
import com.vihan.hospital.patient.PatientDtos.DuplicateCheck;
import com.vihan.hospital.patient.PatientDtos.DuplicateCheckRequest;
import com.vihan.hospital.patient.PatientDtos.PatientDetail;
import com.vihan.hospital.patient.PatientDtos.PatientMatch;
import com.vihan.hospital.patient.PatientDtos.PatientSummary;
import com.vihan.hospital.patient.PatientDtos.RegisterPatientRequest;
import com.vihan.hospital.patient.PatientDtos.UpcomingAppointment;
import com.vihan.hospital.patient.PatientDtos.UpdatePatientRequest;
import com.vihan.hospital.patient.PatientDtos.VisitRow;
import com.vihan.hospital.security.CurrentUser;
import com.vihan.hospital.user.AppUser;
import com.vihan.hospital.user.AppUserRepository;
import com.vihan.hospital.visit.VisitRepository;

/**
 * Patients with a network-wide UHID (DESIGN.md §5.7): registration with duplicate checks, edit,
 * search, profile and the non-clinical visit list. Aadhaar is encrypted on the way in and only
 * its last four digits ever leave this class (§10.7). Views, edits and Aadhaar searches are
 * audit-logged (§12.1), never with the number itself.
 */
@Service
public class PatientService {

	/** "Same age ± 2" in the check-in suggestions (DESIGN.md §19.5). */
	private static final int AGE_TOLERANCE = 2;
	private static final int MAX_RESULTS = 50;

	private final PatientRepository patients;
	private final AadhaarCrypto crypto;
	private final BranchLookup branches;
	private final AppUserRepository users;
	private final AppointmentRepository appointments;
	private final VisitRepository visits;
	private final DoctorRepository doctors;
	private final AuditService audit;
	private final Clock clock;

	public PatientService(PatientRepository patients, AadhaarCrypto crypto, BranchLookup branches, AppUserRepository users,
			AppointmentRepository appointments, VisitRepository visits, DoctorRepository doctors, AuditService audit,
			Clock clock) {
		this.patients = patients;
		this.crypto = crypto;
		this.branches = branches;
		this.users = users;
		this.appointments = appointments;
		this.visits = visits;
		this.doctors = doctors;
		this.audit = audit;
		this.clock = clock;
	}

	// ---- register / edit (FR-PT1, FR-PT2, FR-PT5) ----

	@Transactional
	public PatientSummary register(RegisterPatientRequest req) {
		LocalDate today = LocalDate.now(clock);
		String name = tidy(req.fullName());
		String mobile = Mobiles.normalize(req.mobile());
		String aadhaar = Aadhaar.normalize(req.aadhaar());
		checkDuplicates(name, mobile, aadhaar, null, Boolean.TRUE.equals(req.confirmDuplicate()));

		boolean estimated = req.dateOfBirth() == null;
		LocalDate dob = estimated ? estimatedDob(req.age(), today) : req.dateOfBirth();
		Long branchId = registeredBranch(req.branch());
		String uhid = "VMH-%02d-%06d".formatted(today.getYear() % 100, patients.nextUhidNumber());
		Patient p = new Patient(uhid, name, dob, estimated, req.gender(), mobile, branchId, LocalDateTime.now(clock));
		p.updateContact(mobileOrNull(req.altMobile()), blankToNull(req.address()), blankToNull(req.city()),
				blankToNull(req.bloodGroup()), blankToNull(req.emergencyName()), mobileOrNull(req.emergencyPhone()));
		storeAadhaar(p, aadhaar);
		patients.save(p);
		audit.record(AuditAction.PATIENT_CREATED, "PATIENT", p.getId(), uhid + (aadhaar != null ? " with Aadhaar" : ""));
		return summary(p, branches.byId(), today);
	}

	@Transactional
	public PatientDetail update(Long id, UpdatePatientRequest req) {
		Patient p = activePatient(id);
		LocalDate today = LocalDate.now(clock);
		String name = tidy(req.fullName());
		String mobile = Mobiles.normalize(req.mobile());
		boolean removeAadhaar = Boolean.TRUE.equals(req.removeAadhaar());
		String aadhaar = removeAadhaar ? null : Aadhaar.normalize(req.aadhaar());
		boolean identityChanged = !name.equalsIgnoreCase(p.getFullName()) || !mobile.equals(p.getMobile());
		checkDuplicates(name, mobile, aadhaar, p.getId(), Boolean.TRUE.equals(req.confirmDuplicate()) || !identityChanged);

		// Keep an estimated birth date while the age shown is unchanged, so saving doesn't move it.
		LocalDate dob;
		boolean estimated;
		if (req.dateOfBirth() != null) {
			dob = req.dateOfBirth();
			estimated = false;
		}
		else if (req.age() == p.ageOn(today)) {
			dob = p.getDateOfBirth();
			estimated = p.isDobEstimated();
		}
		else {
			dob = estimatedDob(req.age(), today);
			estimated = true;
		}

		List<String> changed = new ArrayList<>();
		note(changed, "name", p.getFullName(), name);
		note(changed, "dateOfBirth", p.getDateOfBirth(), dob);
		note(changed, "gender", p.getGender(), req.gender());
		note(changed, "mobile", p.getMobile(), mobile);
		note(changed, "altMobile", p.getAltMobile(), mobileOrNull(req.altMobile()));
		note(changed, "address", p.getAddress(), blankToNull(req.address()));
		note(changed, "city", p.getCity(), blankToNull(req.city()));
		note(changed, "bloodGroup", p.getBloodGroup(), blankToNull(req.bloodGroup()));
		note(changed, "emergencyContact", p.getEmergencyName() + "|" + p.getEmergencyPhone(),
				blankToNull(req.emergencyName()) + "|" + mobileOrNull(req.emergencyPhone()));

		p.updateCore(name, dob, estimated, req.gender(), mobile);
		p.updateContact(mobileOrNull(req.altMobile()), blankToNull(req.address()), blankToNull(req.city()),
				blankToNull(req.bloodGroup()), blankToNull(req.emergencyName()), mobileOrNull(req.emergencyPhone()));
		if (removeAadhaar && p.getAadhaarHash() != null) {
			p.setAadhaar(null, null, null);
			changed.add("aadhaar removed");
		}
		else if (aadhaar != null && !crypto.hash(aadhaar).equals(p.getAadhaarHash())) {
			storeAadhaar(p, aadhaar);
			changed.add("aadhaar");
		}
		if (!changed.isEmpty()) {
			// Field names only: the values are personal data and stay out of the log.
			audit.record(AuditAction.PATIENT_UPDATED, "PATIENT", p.getId(), p.getUhid() + ": " + String.join(", ", changed));
		}
		return detail(p, today);
	}

	/** FR-PT2, also offered to the form before saving: same Aadhaar blocks, same mobile + similar name warns. */
	@Transactional(readOnly = true)
	public DuplicateCheck duplicates(DuplicateCheckRequest req) {
		LocalDate today = LocalDate.now(clock);
		Map<Long, Branch> branchMap = branches.byId();
		String aadhaar = Aadhaar.normalize(req.aadhaar());
		PatientSummary aadhaarMatch = aadhaar == null ? null
				: patients.findByAadhaarHash(crypto.hash(aadhaar))
					.filter(p -> !p.getId().equals(req.excludeId()))
					.map(p -> summary(p, branchMap, today))
					.orElse(null);
		String mobile = Mobiles.normalize(req.mobile());
		List<PatientSummary> similar = mobile == null || req.name() == null || req.name().isBlank() ? List.of()
				: similarByMobile(tidy(req.name()), mobile, req.excludeId()).stream().map(p -> summary(p, branchMap, today)).toList();
		return new DuplicateCheck(aadhaarMatch, similar);
	}

	// ---- search and view (FR-PT3, FR-PT4) ----

	/** One of UHID (exact), mobile (exact) or name (contains); the first one given is used. */
	@Transactional(readOnly = true)
	public List<PatientSummary> search(String uhid, String mobile, String name) {
		List<Patient> found;
		if (uhid != null && !uhid.isBlank()) {
			found = patients.findByUhidAndActiveTrue(normalizeUhid(uhid)).map(List::of).orElse(List.of());
		}
		else if (mobile != null && !mobile.isBlank()) {
			String normalized = Mobiles.normalize(mobile);
			if (normalized == null) {
				throw new BadRequestException("INVALID_MOBILE", "Enter a 10-digit mobile number");
			}
			found = patients.findTop20ByActiveTrueAndMobileOrderByFullName(normalized);
		}
		else if (name != null && name.strip().length() >= 2) {
			found = patients.searchByName(name.strip(), PageRequest.of(0, MAX_RESULTS));
		}
		else {
			throw new BadRequestException("SEARCH_REQUIRED", "Enter a UHID, a mobile number or at least 2 letters of the name");
		}
		return summaries(found);
	}

	/** Exact match through the HMAC hash; audited, without the number. */
	@Transactional
	public List<PatientSummary> searchByAadhaar(String aadhaar) {
		List<Patient> found = patients.findByAadhaarHash(crypto.hash(Aadhaar.normalize(aadhaar)))
			.filter(Patient::isActive).map(List::of).orElse(List.of());
		audit.record(AuditAction.PATIENT_SEARCHED, "PATIENT", found.isEmpty() ? null : found.getFirst().getId(),
				"by Aadhaar: " + (found.isEmpty() ? "not found" : found.getFirst().getUhid()));
		return summaries(found);
	}

	/** The profile; every view is audited (DESIGN.md §12.1). */
	@Transactional
	public PatientDetail view(Long id) {
		Patient p = activePatient(id);
		audit.record(AuditAction.PATIENT_VIEWED, "PATIENT", p.getId(), p.getUhid());
		return detail(p, LocalDate.now(clock));
	}

	@Transactional(readOnly = true)
	public List<VisitRow> visitList(Long id) {
		activePatient(id);
		Map<Long, Branch> branchMap = branches.byId();
		Map<Long, Doctor> doctorMap = doctorMap();
		return visits.findByPatientIdOrderByVisitDateDescIdDesc(id).stream()
			.map(v -> {
				Doctor d = doctorMap.get(v.getDoctorId());
				Branch b = branchMap.get(v.getBranchId());
				return new VisitRow(v.getId(), v.getVisitDate(), b == null ? null : b.getCity(), d == null ? null : d.getFullName(),
						d == null ? null : d.getSpeciality().getName(), v.getVisitType(), v.getStatus());
			})
			.toList();
	}

	// ---- used by reception and booking ----

	/** Same mobile, or the same name with any mobile; same-mobile first. */
	@Transactional(readOnly = true)
	public List<PatientMatch> candidates(String name, String mobile, Integer age) {
		LocalDate today = LocalDate.now(clock);
		Map<Long, Branch> branchMap = branches.byId();
		return patients.findCandidates(mobile, name.strip()).stream()
			.limit(20)
			.map(p -> new PatientMatch(summary(p, branchMap, today), p.getMobile().equals(mobile),
					NameMatcher.similar(p.getFullName(), name),
					age != null && Math.abs(p.ageOn(today) - age) <= AGE_TOLERANCE))
			.toList();
	}

	@Transactional(readOnly = true)
	public Patient activePatient(Long id) {
		return patients.findById(id).filter(Patient::isActive).orElseThrow(() -> new NotFoundException("Patient", id));
	}

	/** For booking with a UHID (DESIGN.md §19.5). */
	@Transactional(readOnly = true)
	public Optional<Patient> byUhid(String uhid) {
		return patients.findByUhidAndActiveTrue(normalizeUhid(uhid));
	}

	@Transactional(readOnly = true)
	public String uhidOf(Long patientId) {
		return patients.findById(patientId).map(Patient::getUhid).orElse(null);
	}

	public PatientSummary summary(Patient p, Map<Long, Branch> branchMap, LocalDate today) {
		Branch b = branchMap.get(p.getRegisteredBranchId());
		return new PatientSummary(p.getId(), p.getUhid(), p.getFullName(), p.ageOn(today), p.getDateOfBirth(),
				p.isDobEstimated(), p.getGender(), p.getMobile(), b == null ? null : b.getCode(),
				Masking.aadhaarFromLast4(p.getAadhaarLast4()));
	}

	// ---- helpers ----

	/** {@code vmh 26 000123}, {@code VMH26000123} → {@code VMH-26-000123}. */
	static String normalizeUhid(String input) {
		String s = input.toUpperCase(Locale.ROOT).replaceAll("[\\s-]", "");
		return s.matches("VMH\\d{8}") ? "VMH-" + s.substring(3, 5) + "-" + s.substring(5) : s;
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

	private void checkDuplicates(String name, String mobile, String aadhaar, Long excludeId, boolean similarConfirmed) {
		if (aadhaar != null) {
			patients.findByAadhaarHash(crypto.hash(aadhaar))
				.filter(p -> !p.getId().equals(excludeId))
				.ifPresent(p -> {
					throw new ConflictException("AADHAAR_EXISTS", "This Aadhaar number is already registered as " + p.getUhid());
				});
		}
		if (!similarConfirmed) {
			List<Patient> similar = similarByMobile(name, mobile, excludeId);
			if (!similar.isEmpty()) {
				throw new ConflictException("POSSIBLE_DUPLICATE", "Already registered with this mobile and a similar name: "
						+ similar.stream().map(p -> p.getFullName() + " (" + p.getUhid() + ")").collect(Collectors.joining(", "))
						+ ". Open that record, or confirm this is a different person.");
			}
		}
	}

	private List<Patient> similarByMobile(String name, String mobile, Long excludeId) {
		return patients.findTop20ByActiveTrueAndMobileOrderByFullName(mobile).stream()
			.filter(p -> !p.getId().equals(excludeId) && NameMatcher.similar(p.getFullName(), name))
			.toList();
	}

	private void storeAadhaar(Patient p, String aadhaar) {
		if (aadhaar != null) {
			p.setAadhaar(crypto.encrypt(aadhaar), crypto.hash(aadhaar), aadhaar.substring(8));
		}
	}

	private PatientDetail detail(Patient p, LocalDate today) {
		Map<Long, Branch> branchMap = branches.byId();
		Map<Long, Doctor> doctorMap = doctorMap();
		Branch b = branchMap.get(p.getRegisteredBranchId());
		List<UpcomingAppointment> upcoming = appointments.findUpcomingForPatient(p.getId(), today).stream()
			.map(a -> {
				Doctor d = doctorMap.get(a.getDoctorId());
				Branch ab = branchMap.get(a.getBranchId());
				return new UpcomingAppointment(a.getId(), a.getBookingRef(), a.getApptDate(), a.getStartAt(), a.getTokenNo(),
						d == null ? null : d.getFullName(), ab == null ? null : ab.getCity(), a.getStatus());
			})
			.toList();
		return new PatientDetail(p.getId(), p.getUhid(), p.getFullName(), p.ageOn(today), p.getDateOfBirth(),
				p.isDobEstimated(), p.getGender(), p.getMobile(), p.getAltMobile(), Masking.aadhaarFromLast4(p.getAadhaarLast4()),
				p.getAddress(), p.getCity(), p.getBloodGroup(), p.getEmergencyName(), p.getEmergencyPhone(),
				b == null ? null : b.getCode(), b == null ? null : b.getCity(), p.getCreatedAt(), upcoming);
	}

	private List<PatientSummary> summaries(List<Patient> list) {
		LocalDate today = LocalDate.now(clock);
		Map<Long, Branch> branchMap = branches.byId();
		return list.stream().map(p -> summary(p, branchMap, today)).toList();
	}

	private Map<Long, Doctor> doctorMap() {
		return doctors.findAllByOrderByFullNameAsc().stream().collect(Collectors.toMap(Doctor::getId, Function.identity()));
	}

	private Long registeredBranch(String code) {
		Long id = branches.idOf(code);
		if (id != null) {
			return id;
		}
		return CurrentUser.id().flatMap(users::findById).map(AppUser::getHomeBranchId)
			.orElseThrow(() -> new BadRequestException("BRANCH_REQUIRED", "Choose the branch where the patient is registered"));
	}

	private static void note(List<String> changed, String field, Object before, Object after) {
		if (!Objects.equals(before, after)) {
			changed.add(field);
		}
	}

	private static String tidy(String name) {
		return name.strip().replaceAll("\\s+", " ");
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.strip();
	}

	private static String mobileOrNull(String s) {
		return s == null || s.isBlank() ? null : Mobiles.normalize(s);
	}

}
