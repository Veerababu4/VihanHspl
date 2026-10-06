package com.vihan.hospital.doctor;

import java.math.BigDecimal;
import java.time.Year;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.vihan.hospital.branch.Branch;
import com.vihan.hospital.common.entity.AuditableEntity;
import com.vihan.hospital.speciality.Speciality;

@Entity
@Table(name = "doctor")
public class Doctor extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "doctor_id")
	private Long id;

	@Column(name = "full_name", nullable = false, length = 120)
	private String fullName;

	@Column(length = 1)
	private String gender;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "speciality_id", nullable = false)
	private Speciality speciality;

	@Column(nullable = false, length = 300)
	private String qualification;

	@Column(length = 120)
	private String designation;

	@Column(name = "registration_no", length = 40)
	private String registrationNo;

	@Column(name = "practice_start_year", nullable = false)
	private int practiceStartYear;

	@Column(length = 200)
	private String languages;

	@Column(length = 2000)
	private String bio;

	@Column(name = "photo_url", length = 500)
	private String photoUrl;

	@Column(name = "consultation_fee", precision = 8, scale = 2)
	private BigDecimal consultationFee;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "doctor_branch",
			joinColumns = @JoinColumn(name = "doctor_id"),
			inverseJoinColumns = @JoinColumn(name = "branch_id"))
	private Set<Branch> branches = new HashSet<>();

	protected Doctor() {
	}

	public Doctor(String fullName, Speciality speciality, String qualification, int practiceStartYear) {
		this.fullName = fullName;
		this.speciality = speciality;
		this.qualification = qualification;
		this.practiceStartYear = practiceStartYear;
	}

	/** Years since starting practice; computed so it stays correct every year. */
	public int experienceYears() {
		return Math.max(0, Year.now().getValue() - practiceStartYear);
	}

	public void updateProfile(String fullName, String gender, Speciality speciality, String qualification,
			String designation, String registrationNo, int practiceStartYear, String languages, String bio,
			BigDecimal consultationFee, boolean active) {
		this.fullName = fullName;
		this.gender = gender;
		this.speciality = speciality;
		this.qualification = qualification;
		this.designation = designation;
		this.registrationNo = registrationNo;
		this.practiceStartYear = practiceStartYear;
		this.languages = languages;
		this.bio = bio;
		this.consultationFee = consultationFee;
		this.active = active;
	}

	public void replaceBranches(Set<Branch> newBranches) {
		branches.retainAll(newBranches);
		branches.addAll(newBranches);
	}

	public void setPhotoUrl(String photoUrl) {
		this.photoUrl = photoUrl;
	}

	public Long getId() {
		return id;
	}

	public String getFullName() {
		return fullName;
	}

	public String getGender() {
		return gender;
	}

	public Speciality getSpeciality() {
		return speciality;
	}

	public String getQualification() {
		return qualification;
	}

	public String getDesignation() {
		return designation;
	}

	public String getRegistrationNo() {
		return registrationNo;
	}

	public int getPracticeStartYear() {
		return practiceStartYear;
	}

	public String getLanguages() {
		return languages;
	}

	public String getBio() {
		return bio;
	}

	public String getPhotoUrl() {
		return photoUrl;
	}

	public BigDecimal getConsultationFee() {
		return consultationFee;
	}

	public boolean isActive() {
		return active;
	}

	public Set<Branch> getBranches() {
		return branches;
	}

}
