package com.vihan.hospital.clinical;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.vihan.hospital.common.entity.AuditableEntity;

/** One medicine on a visit's prescription (FR-C2), e.g. "Paracetamol · 500 mg · 1-0-1 · 5 days · after food". */
@Entity
@Table(name = "prescription_item")
public class PrescriptionItem extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "item_id")
	private Long id;

	@Column(name = "visit_id", nullable = false)
	private Long visitId;

	@Column(name = "medicine_name", nullable = false, length = 200)
	private String medicineName;

	@Column(length = 60)
	private String dosage;

	@Column(length = 60)
	private String frequency;

	@Column(length = 60)
	private String duration;

	@Column(length = 300)
	private String instructions;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected PrescriptionItem() {
	}

	public PrescriptionItem(Long visitId, int sortOrder, String medicineName, String dosage, String frequency, String duration,
			String instructions) {
		this.visitId = visitId;
		this.sortOrder = sortOrder;
		this.medicineName = medicineName;
		this.dosage = dosage;
		this.frequency = frequency;
		this.duration = duration;
		this.instructions = instructions;
	}

	public Long getVisitId() {
		return visitId;
	}

	public String getMedicineName() {
		return medicineName;
	}

	public String getDosage() {
		return dosage;
	}

	public String getFrequency() {
		return frequency;
	}

	public String getDuration() {
		return duration;
	}

	public String getInstructions() {
		return instructions;
	}

}
