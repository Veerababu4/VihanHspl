package com.vihan.hospital.appointment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.vihan.hospital.common.util.Mobiles.IndianMobile;

public final class AppointmentDtos {

	private AppointmentDtos() {
	}

	// ---- free slots (public) ----

	/** One day in the booking window with how many slots are still free. */
	public record SlotDayDto(LocalDate date, int openSlots) {
	}

	public record PublicSlotDto(Long slotId, LocalDateTime startAt, LocalDateTime endAt, int tokenNo) {
	}

	// ---- requests ----

	/** Online booking (FR-B1). The privacy consent box must be ticked (DESIGN.md §19.1). */
	public record BookingRequest(
			@NotNull Long slotId,
			@NotBlank @Size(min = 2, max = 120) String patientName,
			@NotNull @Min(0) @Max(120) Integer patientAge,
			@NotBlank @Pattern(regexp = "[MFO]", message = "must be M, F or O") String patientGender,
			@NotBlank @IndianMobile String mobile,
			@Size(max = 500) String reason,
			@AssertTrue(message = "please accept the privacy notice") boolean consent,
			String captchaToken) {
	}

	/** Counter / phone booking by reception (FR-R3): no captcha, channel recorded. */
	public record StaffBookingRequest(
			@NotNull Long slotId,
			@NotBlank @Size(min = 2, max = 120) String patientName,
			@NotNull @Min(0) @Max(120) Integer patientAge,
			@NotBlank @Pattern(regexp = "[MFO]", message = "must be M, F or O") String patientGender,
			@NotBlank @IndianMobile String mobile,
			@Size(max = 500) String reason,
			@NotNull @Pattern(regexp = "COUNTER|PHONE", message = "must be COUNTER or PHONE") String channel,
			@AssertTrue(message = "the patient must agree to the privacy notice") boolean consent) {
	}

	public record LookupRequest(@NotBlank @Size(max = 30) String bookingRef, @NotBlank @Size(max = 20) String mobile) {
	}

	public record CancelRequest(@NotBlank @Size(max = 30) String bookingRef, @NotBlank @Size(max = 20) String mobile,
			@Size(max = 300) String reason, String captchaToken) {
	}

	public record RescheduleRequest(@NotBlank @Size(max = 30) String bookingRef, @NotBlank @Size(max = 20) String mobile,
			@NotNull Long newSlotId, String captchaToken) {
	}

	// ---- the booking as shown to the patient ----

	public record DoctorInfo(Long id, String fullName, String speciality, BigDecimal consultationFee) {
	}

	public record BranchInfo(String code, String name, String city, String address, String phone) {
	}

	/**
	 * Confirmation and "check my appointment" view (FR-B2, FR-B3).
	 *
	 * @param changeDeadline last moment the patient may cancel / reschedule online
	 * @param needsAction    the doctor became unavailable; the hospital will call
	 */
	public record AppointmentView(String bookingRef, Appointment.Status status, Integer tokenNo, LocalDateTime startAt,
			LocalDateTime endAt, DoctorInfo doctor, BranchInfo branch, String patientName, Integer patientAge,
			String patientGender, String mobileMasked, String reason, Appointment.Channel channel, int rescheduleCount,
			int reschedulesLeft, boolean canCancel, boolean canReschedule, LocalDateTime changeDeadline,
			boolean needsAction, String cancelReason) {
	}

}
