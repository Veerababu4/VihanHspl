package com.vihan.hospital.clinical;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.vihan.hospital.common.entity.AuditableEntity;

/**
 * A patient's standing medical facts (FR-C4), shared by all doctors: allergies, chronic
 * conditions, past surgeries, current medications, family history. Doctor-only (§4).
 */
@Entity
@Table(name = "patient_medical_profile")
public class MedicalProfile extends AuditableEntity {

	@Id
	@Column(name = "patient_id")
	private Long patientId;

	@Column(length = 1000)
	private String allergies;

	@Column(name = "chronic_conditions", length = 1000)
	private String chronicConditions;

	@Column(name = "past_surgeries", length = 1000)
	private String pastSurgeries;

	@Column(name = "current_medications", length = 1000)
	private String currentMedications;

	@Column(name = "family_history", length = 1000)
	private String familyHistory;

	protected MedicalProfile() {
	}

	public MedicalProfile(Long patientId) {
		this.patientId = patientId;
	}

	public void update(String allergies, String chronicConditions, String pastSurgeries, String currentMedications,
			String familyHistory) {
		this.allergies = allergies;
		this.chronicConditions = chronicConditions;
		this.pastSurgeries = pastSurgeries;
		this.currentMedications = currentMedications;
		this.familyHistory = familyHistory;
	}

	public Long getPatientId() {
		return patientId;
	}

	public String getAllergies() {
		return allergies;
	}

	public String getChronicConditions() {
		return chronicConditions;
	}

	public String getPastSurgeries() {
		return pastSurgeries;
	}

	public String getCurrentMedications() {
		return currentMedications;
	}

	public String getFamilyHistory() {
		return familyHistory;
	}

}
