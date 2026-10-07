package com.vihan.hospital.appointment;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Reception's appointment list and workflow (DESIGN.md §5.5, §11.4). */
public final class ReceptionDtos {

	private ReceptionDtos() {
	}

	/** What the logged-in user may do with this appointment right now; the screen shows only these buttons. */
	public record Actions(boolean checkIn, boolean cancel, boolean noShow, boolean complete, boolean reschedule) {
	}

	/**
	 * One appointment as staff see it: full mobile number (unlike the patient's own view).
	 *
	 * @param frequentNoShow this mobile missed 3 or more appointments in the last 30 days
	 * @param visitId        the visit opened at check-in (the doctor opens it from the list)
	 */
	public record StaffAppointmentRow(Long id, String bookingRef, Appointment.Status status, Integer tokenNo,
			LocalDate date, LocalDateTime startAt, Long doctorId, String doctorName, String speciality, String branchCode,
			String branchCity, Long patientId, String uhid, String patientName, Integer patientAge, String patientGender,
			String mobile, boolean frequentNoShow, String reason, Appointment.Channel channel, boolean needsAction,
			String cancelReason, Appointment.CancelledBy cancelledBy, int rescheduleCount, LocalDateTime checkedInAt,
			Long visitId, Actions actions) {
	}

	/** Check-in has its own endpoint, because it needs a patient. */
	public record StatusChangeRequest(
			@NotBlank @Pattern(regexp = "NO_SHOW|CANCELLED|COMPLETED", message = "must be NO_SHOW, CANCELLED or COMPLETED") String status,
			@Size(max = 300) String reason) {
	}

	public record CheckInRequest(@NotNull Long patientId) {
	}

	/** FR-R5: a registered patient seen today without a booking. */
	public record WalkInRequest(@NotNull Long patientId, @NotNull Long doctorId, @NotBlank @Size(max = 10) String branch,
			@Size(max = 500) String reason) {
	}

	/** FR-R6: any open future slot of any doctor; a reason is required. */
	public record StaffRescheduleRequest(@NotNull Long newSlotId, @NotBlank @Size(max = 300) String reason) {
	}

}
