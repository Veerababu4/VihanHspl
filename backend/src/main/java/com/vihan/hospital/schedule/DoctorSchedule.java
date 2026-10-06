package com.vihan.hospital.schedule;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.vihan.hospital.common.entity.AuditableEntity;

/**
 * Weekly schedule template: one session of a doctor at a branch on one weekday (DESIGN.md §5.4).
 * M2 only reads it to show availability on the doctor profile; M4 adds editing and slot generation.
 */
@Entity
@Table(name = "doctor_schedule")
public class DoctorSchedule extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "schedule_id")
	private Long id;

	@Column(name = "doctor_id", nullable = false)
	private Long doctorId;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	/** ISO day: 1 = Monday … 7 = Sunday. */
	@Column(name = "day_of_week", nullable = false)
	private int dayOfWeek;

	/** 'HH:mm' */
	@Column(name = "start_time", nullable = false, length = 5)
	private String startTime;

	@Column(name = "end_time", nullable = false, length = 5)
	private String endTime;

	@Column(name = "slot_minutes", nullable = false)
	private int slotMinutes;

	@Column(name = "effective_from", nullable = false)
	private LocalDate effectiveFrom;

	@Column(name = "effective_to")
	private LocalDate effectiveTo;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	protected DoctorSchedule() {
	}

	public Long getId() {
		return id;
	}

	public Long getDoctorId() {
		return doctorId;
	}

	public Long getBranchId() {
		return branchId;
	}

	public int getDayOfWeek() {
		return dayOfWeek;
	}

	public String getStartTime() {
		return startTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public int getSlotMinutes() {
		return slotMinutes;
	}

	public LocalDate getEffectiveFrom() {
		return effectiveFrom;
	}

	public LocalDate getEffectiveTo() {
		return effectiveTo;
	}

	public boolean isActive() {
		return active;
	}

}
