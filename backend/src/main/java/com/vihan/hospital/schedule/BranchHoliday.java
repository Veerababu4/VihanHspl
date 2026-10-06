package com.vihan.hospital.schedule;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.vihan.hospital.common.entity.AuditableEntity;

/** A day a branch (or, with no branch, every branch) has no OPD (FR-S5). */
@Entity
@Table(name = "branch_holiday")
public class BranchHoliday extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "holiday_id")
	private Long id;

	/** null = all branches */
	@Column(name = "branch_id")
	private Long branchId;

	@Column(name = "holiday_date", nullable = false)
	private LocalDate holidayDate;

	@Column(nullable = false, length = 120)
	private String name;

	protected BranchHoliday() {
	}

	public BranchHoliday(Long branchId, LocalDate holidayDate, String name) {
		this.branchId = branchId;
		this.holidayDate = holidayDate;
		this.name = name;
	}

	public boolean appliesTo(Long branch) {
		return branchId == null || branchId.equals(branch);
	}

	public Long getId() {
		return id;
	}

	public Long getBranchId() {
		return branchId;
	}

	public LocalDate getHolidayDate() {
		return holidayDate;
	}

	public String getName() {
		return name;
	}

}
