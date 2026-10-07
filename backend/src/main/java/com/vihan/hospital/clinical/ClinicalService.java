package com.vihan.hospital.clinical;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vihan.hospital.appointment.Appointment;
import com.vihan.hospital.appointment.AppointmentRepository;
import com.vihan.hospital.audit.AuditAction;
import com.vihan.hospital.audit.AuditService;
import com.vihan.hospital.branch.Branch;
import com.vihan.hospital.branch.BranchLookup;
import com.vihan.hospital.clinical.ClinicalDtos.BranchHeader;
import com.vihan.hospital.clinical.ClinicalDtos.DoctorHeader;
import com.vihan.hospital.clinical.ClinicalDtos.History;
import com.vihan.hospital.clinical.ClinicalDtos.HistoryVisit;
import com.vihan.hospital.clinical.ClinicalDtos.MedicalProfileDto;
import com.vihan.hospital.clinical.ClinicalDtos.MedicalProfileRequest;
import com.vihan.hospital.clinical.ClinicalDtos.PatientHeader;
import com.vihan.hospital.clinical.ClinicalDtos.Queue;
import com.vihan.hospital.clinical.ClinicalDtos.QueueEntry;
import com.vihan.hospital.clinical.ClinicalDtos.RxItem;
import com.vihan.hospital.clinical.ClinicalDtos.SaveVisitRequest;
import com.vihan.hospital.clinical.ClinicalDtos.VisitDetail;
import com.vihan.hospital.common.error.ApiException.BadRequestException;
import com.vihan.hospital.common.error.ApiException.ConflictException;
import com.vihan.hospital.common.error.ApiException.NotFoundException;
import com.vihan.hospital.doctor.Doctor;
import com.vihan.hospital.doctor.DoctorRepository;
import com.vihan.hospital.patient.Patient;
import com.vihan.hospital.patient.PatientRepository;
import com.vihan.hospital.patient.PatientService;
import com.vihan.hospital.security.CurrentUser;
import com.vihan.hospital.visit.Visit;
import com.vihan.hospital.visit.VisitRepository;

/**
 * The doctor's side (DESIGN.md §5.8): today's queue, the visit record with its prescription,
 * completing a visit, and the patient's health history and medical profile. Any doctor may read
 * any patient's history (§17 #11); only the treating doctor edits an open visit. History views,
 * other doctors' visit views and every change are audited, never with clinical text (§12.1).
 */
@Service
public class ClinicalService {

	private final VisitRepository visits;
	private final PrescriptionItemRepository rx;
	private final MedicalProfileRepository profiles;
	private final AppointmentRepository appointments;
	private final PatientRepository patients;
	private final PatientService patientService;
	private final DoctorRepository doctors;
	private final BranchLookup branches;
	private final AuditService audit;
	private final Clock clock;

	public ClinicalService(VisitRepository visits, PrescriptionItemRepository rx, MedicalProfileRepository profiles,
			AppointmentRepository appointments, PatientRepository patients, PatientService patientService,
			DoctorRepository doctors, BranchLookup branches, AuditService audit, Clock clock) {
		this.visits = visits;
		this.rx = rx;
		this.profiles = profiles;
		this.appointments = appointments;
		this.patients = patients;
		this.patientService = patientService;
		this.doctors = doctors;
		this.branches = branches;
		this.audit = audit;
		this.clock = clock;
	}

	// ---- queue (FR-C1) ----

	/** The doctor's own checked-in patients that day (any branch), waiting ones in token order. */
	@Transactional(readOnly = true)
	public Queue queue(LocalDate date) {
		LocalDate day = date == null ? LocalDate.now(clock) : date;
		List<Appointment> mine = appointments.findForDay(day, null, ownDoctorId(), null).stream()
			.filter(a -> a.getStatus() == Appointment.Status.CHECKED_IN || a.getStatus() == Appointment.Status.COMPLETED)
			.toList();
		if (mine.isEmpty()) {
			return new Queue(day, List.of(), List.of());
		}
		Map<Long, Visit> visitByAppointment = visits.findByAppointmentIdIn(mine.stream().map(Appointment::getId).toList())
			.stream().collect(Collectors.toMap(Visit::getAppointmentId, Function.identity()));
		Set<Long> patientIds = mine.stream().map(Appointment::getPatientId).filter(Objects::nonNull).collect(Collectors.toSet());
		Map<Long, Patient> patientMap = patients.findAllById(patientIds).stream()
			.collect(Collectors.toMap(Patient::getId, Function.identity()));
		Set<Long> withAllergies = profiles.findAllById(patientIds).stream()
			.filter(p -> p.getAllergies() != null && !p.getAllergies().isBlank())
			.map(MedicalProfile::getPatientId).collect(Collectors.toSet());
		Map<Long, Branch> branchMap = branches.byId();

		Comparator<QueueEntry> byToken = Comparator.comparing(QueueEntry::tokenNo, Comparator.nullsLast(Comparator.naturalOrder()));
		List<QueueEntry> entries = mine.stream().map(a -> {
			Patient p = patientMap.get(a.getPatientId());
			Visit v = visitByAppointment.get(a.getId());
			Branch b = branchMap.get(a.getBranchId());
			return new QueueEntry(a.getId(), v == null ? null : v.getId(), a.getTokenNo(), a.getStartAt(), a.getCheckedInAt(),
					a.getChannel(), a.getPatientId(), p == null ? null : p.getUhid(), p == null ? a.getPatientName() : p.getFullName(),
					p == null ? a.getPatientAge() : Integer.valueOf(p.ageOn(day)), p == null ? a.getPatientGender() : p.getGender(),
					a.getReason(), b == null ? null : b.getCity(), v == null ? null : v.getStatus(),
					withAllergies.contains(a.getPatientId()));
		}).toList();
		return new Queue(day,
				entries.stream().filter(e -> e.visitStatus() != Visit.Status.COMPLETED).sorted(byToken).toList(),
				entries.stream().filter(e -> e.visitStatus() == Visit.Status.COMPLETED).sorted(byToken).toList());
	}

	// ---- one visit (FR-C2, FR-C3) ----

	@Transactional
	public VisitDetail visit(Long id) {
		Visit v = find(id);
		Long me = ownDoctorId();
		if (!v.getDoctorId().equals(me)) {
			// Reading another doctor's visit is allowed (§17 #11) and recorded.
			audit.record(AuditAction.VISIT_VIEWED, "VISIT", v.getId(), "patient " + v.getPatientId());
		}
		return detail(v, me);
	}

	@Transactional
	public VisitDetail save(Long id, SaveVisitRequest req) {
		Visit v = editable(id);
		if (req.followUpDate() != null && !req.followUpDate().isAfter(v.getVisitDate())) {
			throw new BadRequestException("BAD_FOLLOW_UP", "The follow-up date must be after the visit date");
		}
		v.record(new Visit.ClinicalNote(req.bpSystolic(), req.bpDiastolic(), req.pulse(), req.temperatureF(), req.spo2(),
				req.weightKg(), req.heightCm(), blankToNull(req.chiefComplaint()), blankToNull(req.diagnosis()),
				blankToNull(req.clinicalNotes()), blankToNull(req.advice()), req.followUpDate()));
		rx.deleteByVisit(v.getId());
		int order = 0;
		for (var item : req.prescription()) {
			rx.save(new PrescriptionItem(v.getId(), order++, item.medicineName().strip(), blankToNull(item.dosage()),
					blankToNull(item.frequency()), blankToNull(item.duration()), blankToNull(item.instructions())));
		}
		audit.record(AuditAction.VISIT_UPDATED, "VISIT", v.getId(), order + " medicines");
		return detail(v, v.getDoctorId());
	}

	/** FR-C3: the visit and its appointment become COMPLETED; nothing can be changed afterwards. */
	@Transactional
	public VisitDetail complete(Long id) {
		Visit v = editable(id);
		v.complete();
		if (v.getAppointmentId() != null) {
			appointments.findById(v.getAppointmentId())
				.filter(a -> a.getStatus() == Appointment.Status.CHECKED_IN)
				.ifPresent(a -> a.complete(LocalDateTime.now(clock)));
		}
		audit.record(AuditAction.VISIT_COMPLETED, "VISIT", v.getId(), null);
		return detail(v, v.getDoctorId());
	}

	// ---- history and medical profile (FR-C4) ----

	@Transactional
	public History history(Long patientId) {
		ownDoctorId();
		Patient p = patientService.activePatient(patientId);
		LocalDate today = LocalDate.now(clock);
		List<Visit> list = visits.findByPatientIdOrderByVisitDateDescIdDesc(patientId);
		Map<Long, List<RxItem>> rxByVisit = rx.findByVisitIdInOrderBySortOrder(list.stream().map(Visit::getId).toList()).stream()
			.collect(Collectors.groupingBy(PrescriptionItem::getVisitId, Collectors.mapping(ClinicalService::item, Collectors.toList())));
		Map<Long, Doctor> doctorMap = doctorMap();
		Map<Long, Branch> branchMap = branches.byId();
		List<HistoryVisit> rows = new ArrayList<>(list.size());
		for (Visit v : list) {
			Doctor d = doctorMap.get(v.getDoctorId());
			Branch b = branchMap.get(v.getBranchId());
			rows.add(new HistoryVisit(v.getId(), v.getVisitDate(), v.getVisitType(), v.getStatus(), d == null ? null : d.getFullName(),
					d == null ? null : d.getSpeciality().getName(), b == null ? null : b.getCity(), v.note(),
					rxByVisit.getOrDefault(v.getId(), List.of())));
		}
		audit.record(AuditAction.HISTORY_VIEWED, "PATIENT", p.getId(), p.getUhid());
		return new History(header(p, today), profileDto(p.getId()), rows);
	}

	@Transactional(readOnly = true)
	public MedicalProfileDto medicalProfile(Long patientId) {
		ownDoctorId();
		patientService.activePatient(patientId);
		return profileDto(patientId);
	}

	@Transactional
	public MedicalProfileDto updateMedicalProfile(Long patientId, MedicalProfileRequest req) {
		ownDoctorId();
		Patient p = patientService.activePatient(patientId);
		MedicalProfile profile = profiles.findById(patientId).orElseGet(() -> new MedicalProfile(patientId));
		List<String> changed = new ArrayList<>();
		note(changed, "allergies", profile.getAllergies(), blankToNull(req.allergies()));
		note(changed, "chronicConditions", profile.getChronicConditions(), blankToNull(req.chronicConditions()));
		note(changed, "pastSurgeries", profile.getPastSurgeries(), blankToNull(req.pastSurgeries()));
		note(changed, "currentMedications", profile.getCurrentMedications(), blankToNull(req.currentMedications()));
		note(changed, "familyHistory", profile.getFamilyHistory(), blankToNull(req.familyHistory()));
		profile.update(blankToNull(req.allergies()), blankToNull(req.chronicConditions()), blankToNull(req.pastSurgeries()),
				blankToNull(req.currentMedications()), blankToNull(req.familyHistory()));
		profiles.saveAndFlush(profile);
		if (!changed.isEmpty()) {
			audit.record(AuditAction.MEDICAL_PROFILE_UPDATED, "PATIENT", p.getId(), p.getUhid() + ": " + String.join(", ", changed));
		}
		return profileDto(patientId);
	}

	// ---- helpers ----

	/** The logged-in doctor's profile id; the URL rule already allows only doctors (SecurityConfig). */
	private static Long ownDoctorId() {
		return CurrentUser.doctorId().orElseThrow(() -> new AccessDeniedException("No doctor profile is linked to this login"));
	}

	private Visit find(Long id) {
		return visits.findById(id).orElseThrow(() -> new NotFoundException("Visit", id));
	}

	/** Only the treating doctor, and only while the visit is open. */
	private Visit editable(Long id) {
		Visit v = find(id);
		if (!v.getDoctorId().equals(ownDoctorId())) {
			throw new AccessDeniedException("Only the treating doctor can change this visit");
		}
		if (v.getStatus() != Visit.Status.OPEN) {
			throw new ConflictException("VISIT_COMPLETED", "This visit is completed and can no longer be changed");
		}
		return v;
	}

	private VisitDetail detail(Visit v, Long me) {
		LocalDate today = LocalDate.now(clock);
		Patient p = patients.findById(v.getPatientId()).orElseThrow();
		Doctor d = doctors.findWithDetailsById(v.getDoctorId()).orElseThrow();
		Branch b = branches.byId().get(v.getBranchId());
		Appointment a = v.getAppointmentId() == null ? null : appointments.findById(v.getAppointmentId()).orElse(null);
		MedicalProfile profile = profiles.findById(p.getId()).orElse(null);
		return new VisitDetail(v.getId(), v.getVisitDate(), v.getVisitType(), v.getStatus(),
				v.getDoctorId().equals(me) && v.getStatus() == Visit.Status.OPEN, header(p, today),
				profile == null ? null : profile.getAllergies(),
				new DoctorHeader(d.getId(), d.getFullName(), d.getSpeciality().getName(), d.getQualification(), d.getRegistrationNo()),
				b == null ? null : new BranchHeader(b.getCode(), b.getName(), b.getCity(), b.getAddress(), b.getPhone()),
				a == null ? null : a.getTokenNo(), a == null ? null : a.getReason(), v.note(),
				rx.findByVisitIdOrderBySortOrder(v.getId()).stream().map(ClinicalService::item).toList());
	}

	private static PatientHeader header(Patient p, LocalDate today) {
		return new PatientHeader(p.getId(), p.getUhid(), p.getFullName(), p.ageOn(today), p.getGender(), p.getMobile());
	}

	private MedicalProfileDto profileDto(Long patientId) {
		return profiles.findById(patientId)
			.map(m -> new MedicalProfileDto(m.getAllergies(), m.getChronicConditions(), m.getPastSurgeries(),
					m.getCurrentMedications(), m.getFamilyHistory(), m.getUpdatedAt() != null ? m.getUpdatedAt() : m.getCreatedAt()))
			.orElse(new MedicalProfileDto(null, null, null, null, null, null));
	}

	private static RxItem item(PrescriptionItem i) {
		return new RxItem(i.getMedicineName(), i.getDosage(), i.getFrequency(), i.getDuration(), i.getInstructions());
	}

	private Map<Long, Doctor> doctorMap() {
		return doctors.findAllByOrderByFullNameAsc().stream().collect(Collectors.toMap(Doctor::getId, Function.identity()));
	}

	private static void note(List<String> changed, String field, Object before, Object after) {
		if (!Objects.equals(before, after)) {
			changed.add(field);
		}
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.strip();
	}

}
