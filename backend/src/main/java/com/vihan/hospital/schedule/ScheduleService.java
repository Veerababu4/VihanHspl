package com.vihan.hospital.schedule;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vihan.hospital.audit.AuditAction;
import com.vihan.hospital.audit.AuditService;
import com.vihan.hospital.branch.Branch;
import com.vihan.hospital.branch.BranchRepository;
import com.vihan.hospital.common.error.ApiException.BadRequestException;
import com.vihan.hospital.common.error.ApiException.ConflictException;
import com.vihan.hospital.common.error.ApiException.NotFoundException;
import com.vihan.hospital.config.AppProperties;
import com.vihan.hospital.doctor.Doctor;
import com.vihan.hospital.doctor.DoctorRepository;
import com.vihan.hospital.schedule.ScheduleDtos.ChangeSummary;
import com.vihan.hospital.schedule.ScheduleDtos.GenerateRequest;
import com.vihan.hospital.schedule.ScheduleDtos.ScheduleChange;
import com.vihan.hospital.schedule.ScheduleDtos.ScheduleDto;
import com.vihan.hospital.schedule.ScheduleDtos.ScheduleRequest;

/** Weekly timetable per doctor per branch (FR-S1, FR-S6) and manual slot generation (FR-S2). */
@Service
@Transactional
public class ScheduleService {

	private final DoctorScheduleRepository schedules;
	private final DoctorRepository doctors;
	private final BranchRepository branches;
	private final SlotReconciler reconciler;
	private final ChangeSummaries summaries;
	private final ScheduleAccess access;
	private final AuditService audit;
	private final Clock clock;
	private final int maxGenerateDays;

	public ScheduleService(DoctorScheduleRepository schedules, DoctorRepository doctors, BranchRepository branches,
			SlotReconciler reconciler, ChangeSummaries summaries, ScheduleAccess access, AuditService audit, Clock clock,
			AppProperties props) {
		this.schedules = schedules;
		this.doctors = doctors;
		this.branches = branches;
		this.reconciler = reconciler;
		this.summaries = summaries;
		this.access = access;
		this.audit = audit;
		this.clock = clock;
		this.maxGenerateDays = props.scheduling().maxGenerateDays();
	}

	@Transactional(readOnly = true)
	public List<ScheduleDto> list(Long doctorId) {
		access.requireView(doctorId);
		Map<Long, Branch> branchById = branches.findAll().stream().collect(Collectors.toMap(Branch::getId, Function.identity()));
		return schedules.findByDoctorIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(doctorId).stream()
			.map(s -> toDto(s, branchById.get(s.getBranchId())))
			.toList();
	}

	public ScheduleChange<ScheduleDto> create(Long doctorId, ScheduleRequest req) {
		access.requireEdit(doctorId);
		DoctorSchedule s = new DoctorSchedule(doctorId);
		Branch branch = validateAndApply(s, findDoctor(doctorId), req);
		schedules.save(s);
		audit.record(AuditAction.SCHEDULE_CREATED, "DOCTOR", doctorId, describe(s, branch));
		return new ScheduleChange<>(toDto(s, branch), reconcile(doctorId));
	}

	public ScheduleChange<ScheduleDto> update(Long scheduleId, ScheduleRequest req) {
		DoctorSchedule s = findActive(scheduleId);
		access.requireEdit(s.getDoctorId());
		Branch branch = validateAndApply(s, findDoctor(s.getDoctorId()), req);
		audit.record(AuditAction.SCHEDULE_UPDATED, "DOCTOR", s.getDoctorId(), describe(s, branch));
		ScheduleDto dto = toDto(s, branch);
		return new ScheduleChange<>(dto, reconcile(s.getDoctorId()));
	}

	public ChangeSummary delete(Long scheduleId) {
		DoctorSchedule s = findActive(scheduleId);
		access.requireEdit(s.getDoctorId());
		s.deactivate();
		audit.record(AuditAction.SCHEDULE_DELETED, "DOCTOR", s.getDoctorId(),
				describe(s, branches.findById(s.getBranchId()).orElse(null)));
		return reconcile(s.getDoctorId());
	}

	/** "Generate" button (FR-S2): creates any missing slots in a date range; safe to press twice. */
	public ChangeSummary generate(Long doctorId, GenerateRequest req) {
		access.requireEdit(doctorId);
		findDoctor(doctorId);
		LocalDate today = LocalDate.now(clock);
		if (req.to().isBefore(req.from())) {
			throw new BadRequestException("INVALID_RANGE", "End date must be on or after the start date");
		}
		if (req.to().isAfter(today.plusDays(maxGenerateDays))) {
			throw new BadRequestException("RANGE_TOO_FAR", "Slots can be generated at most " + maxGenerateDays + " days ahead");
		}
		LocalDate from = req.from().isBefore(today) ? today : req.from();
		ChangeSummary summary = summaries.of(reconciler.reconcile(doctorId, from, req.to()));
		audit.record(AuditAction.SLOTS_GENERATED, "DOCTOR", doctorId,
				req.from() + ".." + req.to() + ": " + summary.slotsCreated() + " created");
		return summary;
	}

	private ChangeSummary reconcile(Long doctorId) {
		schedules.flush();
		return summaries.of(reconciler.reconcileAffected(doctorId, LocalDate.now(clock), LocalDate.now(clock).plusYears(1)));
	}

	private Branch validateAndApply(DoctorSchedule s, Doctor doctor, ScheduleRequest req) {
		Branch branch = doctor.getBranches().stream()
			.filter(b -> b.getId().equals(req.branchId()))
			.findFirst()
			.orElseThrow(() -> new BadRequestException("BRANCH_NOT_SERVED",
					"This doctor does not work at that branch. Add the branch to the doctor's profile first."));
		if (req.endTime().compareTo(req.startTime()) <= 0) {
			throw new BadRequestException("INVALID_TIME", "End time must be after start time");
		}
		long minutes = Duration.between(LocalTime.parse(req.startTime()), LocalTime.parse(req.endTime())).toMinutes();
		if (minutes < req.slotMinutes()) {
			throw new BadRequestException("SESSION_TOO_SHORT", "The session is shorter than one slot");
		}
		if (req.effectiveTo() != null && req.effectiveTo().isBefore(req.effectiveFrom())) {
			throw new BadRequestException("INVALID_RANGE", "'Effective to' must be on or after 'effective from'");
		}
		s.update(req.branchId(), req.dayOfWeek(), req.startTime(), req.endTime(), req.slotMinutes(), req.effectiveFrom(),
				req.effectiveTo());

		// FR-S6: one place at a time, across all branches.
		for (DoctorSchedule other : schedules.findByDoctorIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(doctor.getId())) {
			if (!other.getId().equals(s.getId()) && SlotPlanner.overlaps(s, other)) {
				Branch otherBranch = branches.findById(other.getBranchId()).orElse(null);
				throw new ConflictException("SCHEDULE_OVERLAP", "Overlaps the existing session " + describe(other, otherBranch));
			}
		}
		return branch;
	}

	private Doctor findDoctor(Long doctorId) {
		return doctors.findWithDetailsById(doctorId).orElseThrow(() -> new NotFoundException("Doctor", doctorId));
	}

	private DoctorSchedule findActive(Long scheduleId) {
		return schedules.findById(scheduleId)
			.filter(DoctorSchedule::isActive)
			.orElseThrow(() -> new NotFoundException("Schedule", scheduleId));
	}

	static ScheduleDto toDto(DoctorSchedule s, Branch b) {
		int perSession = (int) (Duration.between(s.start(), s.end()).toMinutes() / s.getSlotMinutes());
		return new ScheduleDto(s.getId(), s.getDoctorId(), s.getBranchId(), b == null ? null : b.getCode(),
				b == null ? null : b.getCity(), s.getDayOfWeek(), s.getStartTime(), s.getEndTime(), s.getSlotMinutes(),
				s.getEffectiveFrom(), s.getEffectiveTo(), perSession);
	}

	/** "Mon 10:00-14:00 at Kodad" */
	private static String describe(DoctorSchedule s, Branch b) {
		String day = DayOfWeek.of(s.getDayOfWeek()).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
		return day + " " + s.getStartTime() + "-" + s.getEndTime() + (b == null ? "" : " at " + b.getCity());
	}

}
