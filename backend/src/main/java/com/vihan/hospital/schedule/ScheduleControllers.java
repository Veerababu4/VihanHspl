package com.vihan.hospital.schedule;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vihan.hospital.schedule.ScheduleDtos.ChangeSummary;
import com.vihan.hospital.schedule.ScheduleDtos.DayStatusRequest;
import com.vihan.hospital.schedule.ScheduleDtos.GenerateRequest;
import com.vihan.hospital.schedule.ScheduleDtos.HolidayDto;
import com.vihan.hospital.schedule.ScheduleDtos.HolidayRequest;
import com.vihan.hospital.schedule.ScheduleDtos.LeaveDto;
import com.vihan.hospital.schedule.ScheduleDtos.LeaveRequest;
import com.vihan.hospital.schedule.ScheduleDtos.ManualSessionRequest;
import com.vihan.hospital.schedule.ScheduleDtos.ScheduleChange;
import com.vihan.hospital.schedule.ScheduleDtos.ScheduleDto;
import com.vihan.hospital.schedule.ScheduleDtos.ScheduleRequest;
import com.vihan.hospital.schedule.ScheduleDtos.SlotDto;
import com.vihan.hospital.schedule.ScheduleDtos.SlotStatusRequest;

/**
 * /api/staff scheduling endpoints (DESIGN.md §11.3). Who may do what is checked in the services
 * (ScheduleAccess): doctors their own schedule, IT Admin everyone's, receptionists read-only.
 * Every change returns a {@link ChangeSummary} so the screen can report affected slots and bookings.
 */
public final class ScheduleControllers {

	private ScheduleControllers() {
	}

	@RestController
	@RequestMapping("/api/staff")
	public static class StaffScheduleController {

		private final ScheduleService schedules;
		private final SlotService slots;
		private final LeaveHolidayService leaves;

		public StaffScheduleController(ScheduleService schedules, SlotService slots, LeaveHolidayService leaves) {
			this.schedules = schedules;
			this.slots = slots;
			this.leaves = leaves;
		}

		// weekly timetable
		@GetMapping("/doctors/{doctorId}/schedules")
		public List<ScheduleDto> listSchedules(@PathVariable Long doctorId) {
			return schedules.list(doctorId);
		}

		@PostMapping("/doctors/{doctorId}/schedules")
		@ResponseStatus(HttpStatus.CREATED)
		public ScheduleChange<ScheduleDto> createSchedule(@PathVariable Long doctorId, @Valid @RequestBody ScheduleRequest req) {
			return schedules.create(doctorId, req);
		}

		@PutMapping("/schedules/{scheduleId}")
		public ScheduleChange<ScheduleDto> updateSchedule(@PathVariable Long scheduleId, @Valid @RequestBody ScheduleRequest req) {
			return schedules.update(scheduleId, req);
		}

		@DeleteMapping("/schedules/{scheduleId}")
		public ChangeSummary deleteSchedule(@PathVariable Long scheduleId) {
			return schedules.delete(scheduleId);
		}

		// slots
		@PostMapping("/doctors/{doctorId}/slots/generate")
		public ChangeSummary generate(@PathVariable Long doctorId, @Valid @RequestBody GenerateRequest req) {
			return schedules.generate(doctorId, req);
		}

		@GetMapping("/doctors/{doctorId}/slots")
		public List<SlotDto> listSlots(@PathVariable Long doctorId,
				@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
				@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
				@RequestParam(required = false) Long branch) {
			return slots.list(doctorId, from, to, branch);
		}

		@PostMapping("/doctors/{doctorId}/slots")
		@ResponseStatus(HttpStatus.CREATED)
		public List<SlotDto> addSession(@PathVariable Long doctorId, @Valid @RequestBody ManualSessionRequest req) {
			return slots.addSession(doctorId, req);
		}

		@PatchMapping("/slots/{slotId}")
		public SlotDto setSlotStatus(@PathVariable Long slotId, @Valid @RequestBody SlotStatusRequest req) {
			return slots.setStatus(slotId, req.status());
		}

		@PostMapping("/doctors/{doctorId}/slots/day-status")
		public List<SlotDto> setDayStatus(@PathVariable Long doctorId, @Valid @RequestBody DayStatusRequest req) {
			return slots.setDayStatus(doctorId, req);
		}

		// leave
		@GetMapping("/doctors/{doctorId}/leaves")
		public List<LeaveDto> listLeaves(@PathVariable Long doctorId) {
			return leaves.listLeaves(doctorId);
		}

		@PostMapping("/doctors/{doctorId}/leaves")
		@ResponseStatus(HttpStatus.CREATED)
		public ScheduleChange<LeaveDto> addLeave(@PathVariable Long doctorId, @Valid @RequestBody LeaveRequest req) {
			return leaves.addLeave(doctorId, req);
		}

		@DeleteMapping("/leaves/{leaveId}")
		public ChangeSummary removeLeave(@PathVariable Long leaveId) {
			return leaves.removeLeave(leaveId);
		}

		// holidays: everyone on staff can see them
		@GetMapping("/holidays")
		public List<HolidayDto> listHolidays() {
			return leaves.listHolidays();
		}

		@PostMapping("/holidays")
		@ResponseStatus(HttpStatus.CREATED)
		@PreAuthorize("hasRole('IT_ADMIN')")
		public ScheduleChange<HolidayDto> addHoliday(@Valid @RequestBody HolidayRequest req) {
			return leaves.addHoliday(req);
		}

		@DeleteMapping("/holidays/{holidayId}")
		@PreAuthorize("hasRole('IT_ADMIN')")
		public ChangeSummary removeHoliday(@PathVariable Long holidayId) {
			return leaves.removeHoliday(holidayId);
		}

	}

}
