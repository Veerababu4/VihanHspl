package com.vihan.hospital.schedule;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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
import com.vihan.hospital.doctor.Doctor;
import com.vihan.hospital.doctor.DoctorRepository;
import com.vihan.hospital.schedule.ScheduleDtos.DayStatusRequest;
import com.vihan.hospital.schedule.ScheduleDtos.ManualSessionRequest;
import com.vihan.hospital.schedule.ScheduleDtos.SlotDto;
import com.vihan.hospital.schedule.SlotPlanner.Closures;

/** Viewing slots and per-day changes: extra sessions, blocking a slot or a whole day (FR-S3). */
@Service
@Transactional
public class SlotService {

	private static final int MAX_LIST_DAYS = 62;

	private final SlotRepository slots;
	private final DoctorRepository doctors;
	private final BranchRepository branches;
	private final DoctorLeaveRepository leaves;
	private final BranchHolidayRepository holidays;
	private final ScheduleAccess access;
	private final AuditService audit;
	private final Clock clock;

	public SlotService(SlotRepository slots, DoctorRepository doctors, BranchRepository branches,
			DoctorLeaveRepository leaves, BranchHolidayRepository holidays, ScheduleAccess access, AuditService audit,
			Clock clock) {
		this.slots = slots;
		this.doctors = doctors;
		this.branches = branches;
		this.leaves = leaves;
		this.holidays = holidays;
		this.access = access;
		this.audit = audit;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<SlotDto> list(Long doctorId, LocalDate from, LocalDate to, Long branchId) {
		access.requireView(doctorId);
		if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > MAX_LIST_DAYS) {
			throw new BadRequestException("INVALID_RANGE", "Choose a range of at most " + MAX_LIST_DAYS + " days");
		}
		Map<Long, Branch> branchById = branchMap();
		return slots.findByDoctorIdAndSlotDateBetweenOrderByStartAt(doctorId, from, to).stream()
			.filter(s -> branchId == null || branchId.equals(s.getBranchId()))
			.map(s -> toDto(s, branchById))
			.toList();
	}

	/** Extra session on one day, outside the weekly timetable (e.g. a special camp). */
	public List<SlotDto> addSession(Long doctorId, ManualSessionRequest req) {
		access.requireEdit(doctorId);
		Doctor doctor = doctors.findWithDetailsById(doctorId).orElseThrow(() -> new NotFoundException("Doctor", doctorId));
		if (doctor.getBranches().stream().noneMatch(b -> b.getId().equals(req.branchId()) && b.isActive())) {
			throw new BadRequestException("BRANCH_NOT_SERVED", "This doctor does not work at that branch");
		}
		LocalDateTime start = req.date().atTime(LocalTime.parse(req.startTime()));
		LocalDateTime end = req.date().atTime(LocalTime.parse(req.endTime()));
		if (!end.isAfter(start) || start.plusMinutes(req.slotMinutes()).isAfter(end)) {
			throw new BadRequestException("INVALID_TIME", "End time must be at least one slot after the start time");
		}
		if (!start.isAfter(LocalDateTime.now(clock))) {
			throw new BadRequestException("IN_THE_PAST", "Sessions can only be added in the future");
		}
		Closures closures = new Closures(leaves.findOverlapping(doctorId, req.date(), req.date()),
				holidays.findByHolidayDate(req.date()));
		Slot.BlockReason closed = closures.reason(req.date(), req.branchId());
		if (closed != null) {
			throw new ConflictException(closed == Slot.BlockReason.LEAVE ? "ON_LEAVE" : "HOLIDAY",
					closed == Slot.BlockReason.LEAVE ? "The doctor is on leave that day" : "The branch is closed that day (holiday)");
		}
		if (slots.existsOverlapping(doctorId, start, end)) {
			throw new ConflictException("SLOT_OVERLAP", "The doctor already has slots in that time (at this or another branch)");
		}

		int token = slots.findMaxToken(doctorId, req.branchId(), req.date()).orElse(0);
		List<Slot> created = new ArrayList<>();
		for (LocalDateTime t = start; !t.plusMinutes(req.slotMinutes()).isAfter(end); t = t.plusMinutes(req.slotMinutes())) {
			created.add(new Slot(doctorId, req.branchId(), t, t.plusMinutes(req.slotMinutes()), ++token, Slot.Source.MANUAL));
		}
		slots.saveAll(created);
		audit.record(AuditAction.SLOTS_ADDED, "DOCTOR", doctorId,
				req.date() + " " + req.startTime() + "-" + req.endTime() + ", " + created.size() + " slots");
		Map<Long, Branch> branchById = branchMap();
		return created.stream().map(s -> toDto(s, branchById)).toList();
	}

	/** Block or reopen a single slot. Booked slots can't be changed here (cancel the booking instead). */
	public SlotDto setStatus(Long slotId, Slot.Status status) {
		Slot slot = slots.findById(slotId).orElseThrow(() -> new NotFoundException("Slot", slotId));
		access.requireEdit(slot.getDoctorId());
		if (!slot.getStartAt().isAfter(LocalDateTime.now(clock))) {
			throw new BadRequestException("IN_THE_PAST", "Past slots can't be changed");
		}
		switch (status) {
			case BLOCKED -> {
				if (slot.getStatus() == Slot.Status.BOOKED) {
					throw new ConflictException("SLOT_BOOKED", "This slot is booked. Cancel or move the appointment first.");
				}
				slot.block(Slot.BlockReason.MANUAL);
				audit.record(AuditAction.SLOT_BLOCKED, "SLOT", slotId, slot.getStartAt().toString());
			}
			case OPEN -> {
				if (slot.getStatus() == Slot.Status.BOOKED) {
					throw new ConflictException("SLOT_BOOKED", "This slot is booked");
				}
				if (slot.getStatus() == Slot.Status.BLOCKED && slot.getBlockReason() != Slot.BlockReason.MANUAL) {
					throw new ConflictException("BLOCKED_BY_" + slot.getBlockReason(), switch (slot.getBlockReason()) {
						case LEAVE -> "Blocked because the doctor is on leave. Remove the leave instead.";
						case HOLIDAY -> "Blocked because of a holiday. Remove the holiday instead.";
						default -> "This slot is no longer in the weekly timetable.";
					});
				}
				slot.open();
				audit.record(AuditAction.SLOT_UNBLOCKED, "SLOT", slotId, slot.getStartAt().toString());
			}
			case BOOKED -> throw new BadRequestException("INVALID_STATUS", "Slots are booked through appointments");
		}
		return toDto(slot, branchMap());
	}

	/** Block (or reopen manually blocked) every future non-booked slot of the doctor on a date. */
	public List<SlotDto> setDayStatus(Long doctorId, DayStatusRequest req) {
		access.requireEdit(doctorId);
		LocalDateTime now = LocalDateTime.now(clock);
		int changed = 0;
		for (Slot s : slots.findByDoctorIdAndSlotDateBetweenOrderByStartAt(doctorId, req.date(), req.date())) {
			if (!s.getStartAt().isAfter(now)) {
				continue;
			}
			if (req.blocked() && s.getStatus() == Slot.Status.OPEN) {
				s.block(Slot.BlockReason.MANUAL);
				changed++;
			}
			else if (!req.blocked() && s.getStatus() == Slot.Status.BLOCKED && s.getBlockReason() == Slot.BlockReason.MANUAL) {
				s.open();
				changed++;
			}
		}
		audit.record(req.blocked() ? AuditAction.DAY_BLOCKED : AuditAction.DAY_UNBLOCKED, "DOCTOR", doctorId,
				req.date() + ": " + changed + " slots");
		return list(doctorId, req.date(), req.date(), null);
	}

	private Map<Long, Branch> branchMap() {
		return branches.findAll().stream().collect(Collectors.toMap(Branch::getId, Function.identity()));
	}

	static SlotDto toDto(Slot s, Map<Long, Branch> branchById) {
		Branch b = branchById.get(s.getBranchId());
		return new SlotDto(s.getId(), s.getBranchId(), b == null ? null : b.getCode(), s.getSlotDate(), s.getStartAt(),
				s.getEndAt(), s.getTokenNo(), s.getStatus(), s.getSource(), s.getBlockReason());
	}

}
