package com.vihan.hospital.appointment;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vihan.hospital.appointment.AppointmentDtos.PublicSlotDto;
import com.vihan.hospital.appointment.AppointmentDtos.SlotDayDto;
import com.vihan.hospital.branch.Branch;
import com.vihan.hospital.common.error.ApiException.NotFoundException;
import com.vihan.hospital.config.AppProperties;
import com.vihan.hospital.doctor.Doctor;
import com.vihan.hospital.doctor.DoctorRepository;
import com.vihan.hospital.schedule.Slot;
import com.vihan.hospital.schedule.SlotReconciler;
import com.vihan.hospital.schedule.SlotRepository;

/** Free slots for the public booking wizard: only what can actually be booked online right now. */
@Service
@Transactional(readOnly = true)
public class PublicSlotService {

	private final SlotRepository slots;
	private final DoctorRepository doctors;
	private final SlotReconciler reconciler;
	private final Clock clock;
	private final int minLeadMinutes;

	public PublicSlotService(SlotRepository slots, DoctorRepository doctors, SlotReconciler reconciler, Clock clock,
			AppProperties props) {
		this.slots = slots;
		this.doctors = doctors;
		this.reconciler = reconciler;
		this.clock = clock;
		this.minLeadMinutes = props.booking().minLeadMinutes();
	}

	/** Every day of the booking window with its number of free slots (0 = nothing to book that day). */
	public List<SlotDayDto> days(Long doctorId, String branchCode) {
		Branch branch = bookableBranch(doctorId, branchCode);
		LocalDate today = LocalDate.now(clock);
		Map<LocalDate, Integer> counts = new TreeMap<>();
		for (LocalDate d = today; !d.isAfter(reconciler.windowEnd()); d = d.plusDays(1)) {
			counts.put(d, 0);
		}
		for (Slot s : open(doctorId, branch, reconciler.windowEnd())) {
			counts.merge(s.getSlotDate(), 1, Integer::sum);
		}
		return counts.entrySet().stream().map(e -> new SlotDayDto(e.getKey(), e.getValue())).toList();
	}

	public List<PublicSlotDto> slots(Long doctorId, String branchCode, LocalDate date) {
		Branch branch = bookableBranch(doctorId, branchCode);
		if (date.isBefore(LocalDate.now(clock)) || date.isAfter(reconciler.windowEnd())) {
			return List.of();
		}
		return open(doctorId, branch, date).stream()
			.filter(s -> s.getSlotDate().equals(date))
			.map(s -> new PublicSlotDto(s.getId(), s.getStartAt(), s.getEndAt(), s.getTokenNo()))
			.toList();
	}

	private List<Slot> open(Long doctorId, Branch branch, LocalDate toDate) {
		LocalDateTime earliest = LocalDateTime.now(clock).plusMinutes(minLeadMinutes);
		return slots.findOpen(doctorId, branch.getId(), earliest, toDate);
	}

	/** The doctor must be active and serve this (active) branch. */
	private Branch bookableBranch(Long doctorId, String branchCode) {
		Doctor doctor = doctors.findWithDetailsById(doctorId)
			.filter(Doctor::isActive)
			.orElseThrow(() -> new NotFoundException("Doctor", doctorId));
		return doctor.getBranches().stream()
			.filter(b -> b.isActive() && b.getCode().equalsIgnoreCase(branchCode))
			.findFirst()
			.orElseThrow(() -> new NotFoundException("Branch for this doctor", branchCode));
	}

}
