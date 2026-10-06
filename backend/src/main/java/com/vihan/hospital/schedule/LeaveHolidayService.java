package com.vihan.hospital.schedule;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vihan.hospital.audit.AuditAction;
import com.vihan.hospital.audit.AuditService;
import com.vihan.hospital.branch.Branch;
import com.vihan.hospital.branch.BranchRepository;
import com.vihan.hospital.common.error.ApiException.BadRequestException;
import com.vihan.hospital.common.error.ApiException.ConflictException;
import com.vihan.hospital.common.error.ApiException.NotFoundException;
import com.vihan.hospital.doctor.Doctor;
import com.vihan.hospital.doctor.DoctorRepository;
import com.vihan.hospital.schedule.ScheduleDtos.ChangeSummary;
import com.vihan.hospital.schedule.ScheduleDtos.HolidayDto;
import com.vihan.hospital.schedule.ScheduleDtos.HolidayRequest;
import com.vihan.hospital.schedule.ScheduleDtos.LeaveDto;
import com.vihan.hospital.schedule.ScheduleDtos.LeaveRequest;
import com.vihan.hospital.schedule.ScheduleDtos.ScheduleChange;
import com.vihan.hospital.schedule.SlotReconciler.Result;

/**
 * Doctor leave (FR-S4) and branch holidays (FR-S5). Both stop slot generation; existing open slots
 * on those days are blocked, and booked ones are flagged for reception to phone the patient.
 * Removing a leave / holiday reopens exactly the slots it blocked.
 */
@Service
@Transactional
public class LeaveHolidayService {

	private static final int MAX_LEAVE_DAYS = 366;

	private final DoctorLeaveRepository leaves;
	private final BranchHolidayRepository holidays;
	private final DoctorRepository doctors;
	private final BranchRepository branches;
	private final SlotReconciler reconciler;
	private final ChangeSummaries summaries;
	private final ScheduleAccess access;
	private final AuditService audit;
	private final Clock clock;

	public LeaveHolidayService(DoctorLeaveRepository leaves, BranchHolidayRepository holidays, DoctorRepository doctors,
			BranchRepository branches, SlotReconciler reconciler, ChangeSummaries summaries, ScheduleAccess access,
			AuditService audit, Clock clock) {
		this.leaves = leaves;
		this.holidays = holidays;
		this.doctors = doctors;
		this.branches = branches;
		this.reconciler = reconciler;
		this.summaries = summaries;
		this.access = access;
		this.audit = audit;
		this.clock = clock;
	}

	// ---- leave ----

	@Transactional(readOnly = true)
	public List<LeaveDto> listLeaves(Long doctorId) {
		access.requireView(doctorId);
		return leaves.findByDoctorIdAndToDateGreaterThanEqualOrderByFromDate(doctorId, LocalDate.now(clock)).stream()
			.map(LeaveDto::from)
			.toList();
	}

	public ScheduleChange<LeaveDto> addLeave(Long doctorId, LeaveRequest req) {
		access.requireEdit(doctorId);
		doctors.findById(doctorId).orElseThrow(() -> new NotFoundException("Doctor", doctorId));
		if (req.toDate().isBefore(req.fromDate())) {
			throw new BadRequestException("INVALID_RANGE", "'To' must be on or after 'from'");
		}
		if (req.toDate().isBefore(LocalDate.now(clock))) {
			throw new BadRequestException("IN_THE_PAST", "Leave must include today or a future date");
		}
		if (ChronoUnit.DAYS.between(req.fromDate(), req.toDate()) >= MAX_LEAVE_DAYS) {
			throw new BadRequestException("RANGE_TOO_LONG", "Leave can be at most one year at a time");
		}
		if (!leaves.findOverlapping(doctorId, req.fromDate(), req.toDate()).isEmpty()) {
			throw new ConflictException("LEAVE_OVERLAP", "This overlaps leave that is already recorded");
		}
		DoctorLeave leave = leaves.saveAndFlush(new DoctorLeave(doctorId, req.fromDate(), req.toDate(), blankToNull(req.reason())));
		ChangeSummary changes = summaries.of(reconciler.reconcileAffected(doctorId, req.fromDate(), req.toDate()));
		audit.record(AuditAction.LEAVE_ADDED, "DOCTOR", doctorId, req.fromDate() + ".." + req.toDate()
				+ ", bookings to call: " + changes.affectedBookings().size());
		return new ScheduleChange<>(LeaveDto.from(leave), changes);
	}

	public ChangeSummary removeLeave(Long leaveId) {
		DoctorLeave leave = leaves.findById(leaveId).orElseThrow(() -> new NotFoundException("Leave", leaveId));
		access.requireEdit(leave.getDoctorId());
		leaves.delete(leave);
		leaves.flush();
		audit.record(AuditAction.LEAVE_REMOVED, "DOCTOR", leave.getDoctorId(), leave.getFromDate() + ".." + leave.getToDate());
		return summaries.of(reconciler.reconcileAffected(leave.getDoctorId(), leave.getFromDate(), leave.getToDate()));
	}

	// ---- holidays (IT Admin) ----

	@Transactional(readOnly = true)
	public List<HolidayDto> listHolidays() {
		Map<Long, Branch> branchById = branchMap();
		return holidays.findByHolidayDateGreaterThanEqualOrderByHolidayDate(LocalDate.now(clock)).stream()
			.map(h -> toDto(h, branchById))
			.toList();
	}

	public ScheduleChange<HolidayDto> addHoliday(HolidayRequest req) {
		if (req.date().isBefore(LocalDate.now(clock))) {
			throw new BadRequestException("IN_THE_PAST", "Holidays can only be added for today or later");
		}
		if (req.branchId() != null && !branches.existsById(req.branchId())) {
			throw new BadRequestException("UNKNOWN_BRANCH", "Branch does not exist");
		}
		BranchHoliday holiday;
		try {
			holiday = holidays.saveAndFlush(new BranchHoliday(req.branchId(), req.date(), req.name().strip()));
		}
		catch (DataIntegrityViolationException e) {
			throw new ConflictException("HOLIDAY_EXISTS", "A holiday is already recorded for that branch and date");
		}
		ChangeSummary changes = reconcileDoctorsOf(req.branchId(), req.date());
		audit.record(AuditAction.HOLIDAY_ADDED, "BRANCH", req.branchId(), req.date() + " " + req.name()
				+ ", bookings to call: " + changes.affectedBookings().size());
		return new ScheduleChange<>(toDto(holiday, branchMap()), changes);
	}

	public ChangeSummary removeHoliday(Long holidayId) {
		BranchHoliday h = holidays.findById(holidayId).orElseThrow(() -> new NotFoundException("Holiday", holidayId));
		holidays.delete(h);
		holidays.flush();
		audit.record(AuditAction.HOLIDAY_REMOVED, "BRANCH", h.getBranchId(), h.getHolidayDate() + " " + h.getName());
		return reconcileDoctorsOf(h.getBranchId(), h.getHolidayDate());
	}

	/** Every doctor who works at the branch (or anywhere, for an all-branch holiday), for that one day. */
	private ChangeSummary reconcileDoctorsOf(Long branchId, LocalDate date) {
		Set<Long> doctorIds = doctors.findAllByOrderByFullNameAsc().stream()   // branches fetched in the same query
			.filter(d -> branchId == null || d.getBranches().stream().anyMatch(b -> b.getId().equals(branchId)))
			.map(Doctor::getId)
			.collect(Collectors.toSet());
		Result total = Result.EMPTY;
		for (Long id : doctorIds) {
			total = total.plus(reconciler.reconcileAffected(id, date, date));
		}
		return summaries.of(total);
	}

	private Map<Long, Branch> branchMap() {
		return branches.findAll().stream().collect(Collectors.toMap(Branch::getId, Function.identity()));
	}

	private static HolidayDto toDto(BranchHoliday h, Map<Long, Branch> branchById) {
		Branch b = h.getBranchId() == null ? null : branchById.get(h.getBranchId());
		return new HolidayDto(h.getId(), h.getBranchId(), b == null ? null : b.getCode(), h.getHolidayDate(), h.getName());
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.strip();
	}

}
