package com.vihan.hospital.visit;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.vihan.hospital.common.entity.AuditableEntity;

/**
 * One consultation (DESIGN.md §9.3). Created at check-in (M6); the doctor's clinical entry —
 * vitals, diagnosis, notes, prescription — comes with the clinical module (M8).
 */
@Entity
@Table(name = "visit")
public class Visit extends AuditableEntity {

	public enum Type { OP, FOLLOW_UP, EMERGENCY }

	public enum Status { OPEN, COMPLETED }

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "visit_id")
	private Long id;

	@Column(name = "appointment_id")
	private Long appointmentId;

	@Column(name = "patient_id", nullable = false)
	private Long patientId;

	@Column(name = "doctor_id", nullable = false)
	private Long doctorId;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	@Column(name = "visit_date", nullable = false)
	private LocalDate visitDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "visit_type", nullable = false, length = 12)
	private Type visitType = Type.OP;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private Status status = Status.OPEN;

	protected Visit() {
	}

	public Visit(Long appointmentId, Long patientId, Long doctorId, Long branchId, LocalDate visitDate) {
		this.appointmentId = appointmentId;
		this.patientId = patientId;
		this.doctorId = doctorId;
		this.branchId = branchId;
		this.visitDate = visitDate;
	}

	public void complete() {
		this.status = Status.COMPLETED;
	}

	public Long getId() {
		return id;
	}

	public Long getAppointmentId() {
		return appointmentId;
	}

	public Long getPatientId() {
		return patientId;
	}

	public Long getDoctorId() {
		return doctorId;
	}

	public Long getBranchId() {
		return branchId;
	}

	public LocalDate getVisitDate() {
		return visitDate;
	}

	public Type getVisitType() {
		return visitType;
	}

	public Status getStatus() {
		return status;
	}

}
