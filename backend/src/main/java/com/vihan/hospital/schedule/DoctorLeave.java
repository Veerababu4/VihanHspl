package com.vihan.hospital.schedule;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.vihan.hospital.common.entity.AuditableEntity;

/** Doctor away for a date range (FR-S4): no slots at any branch on those days. */
@Entity
@Table(name = "doctor_leave")
public class DoctorLeave extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "leave_id")
	private Long id;

	@Column(name = "doctor_id", nullable = false)
	private Long doctorId;

	@Column(name = "from_date", nullable = false)
	private LocalDate fromDate;

	@Column(name = "to_date", nullable = false)
	private LocalDate toDate;

	@Column(length = 300)
	private String reason;

	protected DoctorLeave() {
	}

	public DoctorLeave(Long doctorId, LocalDate fromDate, LocalDate toDate, String reason) {
		this.doctorId = doctorId;
		this.fromDate = fromDate;
		this.toDate = toDate;
		this.reason = reason;
	}

	public boolean covers(LocalDate date) {
		return !date.isBefore(fromDate) && !date.isAfter(toDate);
	}

	public Long getId() {
		return id;
	}

	public Long getDoctorId() {
		return doctorId;
	}

	public LocalDate getFromDate() {
		return fromDate;
	}

	public LocalDate getToDate() {
		return toDate;
	}

	public String getReason() {
		return reason;
	}

}
