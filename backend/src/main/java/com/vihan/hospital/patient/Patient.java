package com.vihan.hospital.patient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.vihan.hospital.common.entity.AuditableEntity;

/**
 * A registered patient with a network-wide UHID (DESIGN.md §9.3, FR-PT1). M6 registers the core
 * fields; M7 adds Aadhaar (encrypted, §10.7), contact details, search and editing.
 */
@Entity
@Table(name = "patient")
public class Patient extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "patient_id")
	private Long id;

	@Column(nullable = false, length = 20)
	private String uhid;

	@Column(name = "full_name", nullable = false, length = 120)
	private String fullName;

	@Column(name = "date_of_birth", nullable = false)
	private LocalDate dateOfBirth;

	/** Only an age was given: the date of birth is 1 July of the birth year (DESIGN.md §10.6). */
	@Column(name = "dob_is_estimated", nullable = false)
	private boolean dobEstimated;

	@Column(nullable = false, length = 1)
	private String gender;

	@Column(nullable = false, length = 10)
	private String mobile;

	@Column(name = "alt_mobile", length = 10)
	private String altMobile;

	/** AES-256-GCM, see {@link AadhaarCrypto}. Never returned by the API. */
	@Column(name = "aadhaar_enc", length = 200)
	private String aadhaarEnc;

	@Column(name = "aadhaar_hash", length = 64)
	private String aadhaarHash;

	@Column(name = "aadhaar_last4", length = 4)
	private String aadhaarLast4;

	@Column(length = 400)
	private String address;

	@Column(length = 60)
	private String city;

	@Column(name = "blood_group", length = 5)
	private String bloodGroup;

	@Column(name = "emergency_name", length = 120)
	private String emergencyName;

	@Column(name = "emergency_phone", length = 10)
	private String emergencyPhone;

	@Column(name = "registered_branch_id", nullable = false)
	private Long registeredBranchId;

	@Column(name = "consent_at", nullable = false)
	private LocalDateTime consentAt;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	protected Patient() {
	}

	public Patient(String uhid, String fullName, LocalDate dateOfBirth, boolean dobEstimated, String gender, String mobile,
			Long registeredBranchId, LocalDateTime consentAt) {
		this.uhid = uhid;
		this.fullName = fullName;
		this.dateOfBirth = dateOfBirth;
		this.dobEstimated = dobEstimated;
		this.gender = gender;
		this.mobile = mobile;
		this.registeredBranchId = registeredBranchId;
		this.consentAt = consentAt;
	}

	/** Core details (FR-PT5); the registered branch and UHID never change. */
	public void updateCore(String fullName, LocalDate dateOfBirth, boolean dobEstimated, String gender, String mobile) {
		this.fullName = fullName;
		this.dateOfBirth = dateOfBirth;
		this.dobEstimated = dobEstimated;
		this.gender = gender;
		this.mobile = mobile;
	}

	public void updateContact(String altMobile, String address, String city, String bloodGroup, String emergencyName,
			String emergencyPhone) {
		this.altMobile = altMobile;
		this.address = address;
		this.city = city;
		this.bloodGroup = bloodGroup;
		this.emergencyName = emergencyName;
		this.emergencyPhone = emergencyPhone;
	}

	/** All three Aadhaar columns change together; nulls remove the number. */
	public void setAadhaar(String encrypted, String hash, String last4) {
		this.aadhaarEnc = encrypted;
		this.aadhaarHash = hash;
		this.aadhaarLast4 = last4;
	}

	/** Age is always computed, so it stays right over the years (DESIGN.md §10.6). */
	public int ageOn(LocalDate day) {
		return Math.max(0, Period.between(dateOfBirth, day).getYears());
	}

	public Long getId() {
		return id;
	}

	public String getUhid() {
		return uhid;
	}

	public String getFullName() {
		return fullName;
	}

	public LocalDate getDateOfBirth() {
		return dateOfBirth;
	}

	public boolean isDobEstimated() {
		return dobEstimated;
	}

	public String getGender() {
		return gender;
	}

	public String getMobile() {
		return mobile;
	}

	public Long getRegisteredBranchId() {
		return registeredBranchId;
	}

	public boolean isActive() {
		return active;
	}

	public String getAltMobile() {
		return altMobile;
	}

	public String getAadhaarHash() {
		return aadhaarHash;
	}

	public String getAadhaarLast4() {
		return aadhaarLast4;
	}

	public String getAddress() {
		return address;
	}

	public String getCity() {
		return city;
	}

	public String getBloodGroup() {
		return bloodGroup;
	}

	public String getEmergencyName() {
		return emergencyName;
	}

	public String getEmergencyPhone() {
		return emergencyPhone;
	}

	public LocalDateTime getConsentAt() {
		return consentAt;
	}

}
