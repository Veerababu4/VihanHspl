package com.vihan.hospital.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ScheduleDtos {

	private ScheduleDtos() {
	}

	private static final String TIME = "([01]\\d|2[0-3]):[0-5]\\d";
	private static final String TIME_MESSAGE = "must be a time like 09:30";

	// ---- weekly templates (FR-S1) ----

	/** @param slotsPerSession how many slots this session gives, shown in the editor */
	public record ScheduleDto(Long id, Long doctorId, Long branchId, String branchCode, String branchCity, int dayOfWeek,
			String startTime, String endTime, int slotMinutes, LocalDate effectiveFrom, LocalDate effectiveTo,
			int slotsPerSession) {
	}

	public record ScheduleRequest(
			@NotNull Long branchId,
			@NotNull @Min(1) @Max(7) Integer dayOfWeek,
			@NotBlank @Pattern(regexp = TIME, message = TIME_MESSAGE) String startTime,
			@NotBlank @Pattern(regexp = TIME, message = TIME_MESSAGE) String endTime,
			@NotNull @Min(5) @Max(120) Integer slotMinutes,
			@NotNull LocalDate effectiveFrom,
			LocalDate effectiveTo) {
	}

	// ---- slots ----

	public record SlotDto(Long id, Long branchId, String branchCode, LocalDate date, LocalDateTime startAt,
			LocalDateTime endAt, int tokenNo, Slot.Status status, Slot.Source source, Slot.BlockReason blockReason) {
	}

	/** Extra session on one day (FR-S3), split into slots of {@code slotMinutes}. */
	public record ManualSessionRequest(
			@NotNull Long branchId,
			@NotNull LocalDate date,
			@NotBlank @Pattern(regexp = TIME, message = TIME_MESSAGE) String startTime,
			@NotBlank @Pattern(regexp = TIME, message = TIME_MESSAGE) String endTime,
			@NotNull @Min(5) @Max(120) Integer slotMinutes) {
	}

	/** Block or reopen one slot. */
	public record SlotStatusRequest(@NotNull Slot.Status status) {
	}

	/** Block or reopen every non-booked slot of a doctor on one day. */
	public record DayStatusRequest(@NotNull LocalDate date, @NotNull Boolean blocked) {
	}

	public record GenerateRequest(@NotNull LocalDate from, @NotNull LocalDate to) {
	}

	// ---- leave & holidays (FR-S4, FR-S5) ----

	public record LeaveDto(Long id, Long doctorId, LocalDate fromDate, LocalDate toDate, String reason) {
		static LeaveDto from(DoctorLeave l) {
			return new LeaveDto(l.getId(), l.getDoctorId(), l.getFromDate(), l.getToDate(), l.getReason());
		}
	}

	public record LeaveRequest(@NotNull LocalDate fromDate, @NotNull LocalDate toDate, @Size(max = 300) String reason) {
	}

	/** @param branchCode null = all branches */
	public record HolidayDto(Long id, Long branchId, String branchCode, LocalDate date, String name) {
	}

	public record HolidayRequest(Long branchId, @NotNull LocalDate date, @NotBlank @Size(max = 120) String name) {
	}

	// ---- result of a change ----

	/** A booking the receptionist must phone about (mobile masked). */
	public record AffectedBooking(String bookingRef, String patientName, String mobile, LocalDateTime startAt,
			Integer tokenNo, String branchCity) {
	}

	/** What a change did to the slots, so the screen can say "12 slots removed, 2 bookings need a call". */
	public record ChangeSummary(int slotsCreated, int slotsRemoved, int slotsBlocked, int slotsReopened,
			List<AffectedBooking> affectedBookings) {
	}

	public record ScheduleChange<T>(T item, ChangeSummary changes) {
	}

}
