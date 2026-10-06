package com.vihan.hospital.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.vihan.hospital.common.entity.AuditableEntity;

/** One bookable time of a doctor at a branch (DESIGN.md §9.3). */
@Entity
@Table(name = "slot")
public class Slot extends AuditableEntity {

	public enum Status { OPEN, BOOKED, BLOCKED }

	/** TEMPLATE = generated from the weekly timetable; MANUAL = added by a person (extra session). */
	public enum Source { TEMPLATE, MANUAL }

	/** Why a slot is BLOCKED (see V3__scheduling.sql). */
	public enum BlockReason { MANUAL, LEAVE, HOLIDAY, REMOVED }

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "slot_id")
	private Long id;

	@Column(name = "doctor_id", nullable = false)
	private Long doctorId;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	@Column(name = "slot_date", nullable = false)
	private LocalDate slotDate;

	@Column(name = "start_at", nullable = false)
	private LocalDateTime startAt;

	@Column(name = "end_at", nullable = false)
	private LocalDateTime endAt;

	@Column(name = "token_no", nullable = false)
	private int tokenNo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Status status = Status.OPEN;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Source source;

	@Enumerated(EnumType.STRING)
	@Column(name = "block_reason", length = 10)
	private BlockReason blockReason;

	/** Optimistic locking: booking (M5) and blocking can't overwrite each other. */
	@Version
	@Column(nullable = false)
	private long version;

	protected Slot() {
	}

	public Slot(Long doctorId, Long branchId, LocalDateTime startAt, LocalDateTime endAt, int tokenNo, Source source) {
		this.doctorId = doctorId;
		this.branchId = branchId;
		this.slotDate = startAt.toLocalDate();
		this.startAt = startAt;
		this.endAt = endAt;
		this.tokenNo = tokenNo;
		this.source = source;
	}

	public void block(BlockReason reason) {
		if (status == Status.BOOKED) {
			throw new IllegalStateException("A booked slot can't be blocked");
		}
		this.status = Status.BLOCKED;
		this.blockReason = reason;
	}

	public void open() {
		if (status == Status.BOOKED) {
			throw new IllegalStateException("A booked slot can't be reopened");
		}
		this.status = Status.OPEN;
		this.blockReason = null;
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

	public LocalDate getSlotDate() {
		return slotDate;
	}

	public LocalDateTime getStartAt() {
		return startAt;
	}

	public LocalDateTime getEndAt() {
		return endAt;
	}

	public int getTokenNo() {
		return tokenNo;
	}

	public Status getStatus() {
		return status;
	}

	public Source getSource() {
		return source;
	}

	public BlockReason getBlockReason() {
		return blockReason;
	}

}
