package com.vihan.hospital.user;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.vihan.hospital.common.entity.AuditableEntity;

/** A staff login: one account per person (DESIGN.md §19.6). */
@Entity
@Table(name = "app_user")
public class AppUser extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long id;

	@Column(nullable = false, length = 60)
	private String username;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(name = "full_name", nullable = false, length = 120)
	private String fullName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 15)
	private Role role;

	/** Set only for DOCTOR (DB check constraint). */
	@Column(name = "doctor_id")
	private Long doctorId;

	@Column(name = "home_branch_id")
	private Long homeBranchId;

	@Column(length = 10)
	private String mobile;

	@Column(length = 120)
	private String email;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	@Column(name = "must_change_password", nullable = false)
	private boolean mustChangePassword = true;

	@Column(name = "failed_attempts", nullable = false)
	private int failedAttempts;

	@Column(name = "locked_until")
	private LocalDateTime lockedUntil;

	@Column(name = "last_login_at")
	private LocalDateTime lastLoginAt;

	protected AppUser() {
	}

	public AppUser(String username, String passwordHash) {
		this.username = username;
		this.passwordHash = passwordHash;
	}

	public void updateProfile(String fullName, Role role, Long doctorId, Long homeBranchId, String mobile, String email,
			boolean active) {
		this.fullName = fullName;
		this.role = role;
		this.doctorId = doctorId;
		this.homeBranchId = homeBranchId;
		this.mobile = mobile;
		this.email = email;
		this.active = active;
	}

	/** Sets a new password. A temporary one (admin reset / new account) must be changed at next login. */
	public void setPassword(String passwordHash, boolean temporary) {
		this.passwordHash = passwordHash;
		this.mustChangePassword = temporary;
		unlock();
	}

	public boolean isLocked(LocalDateTime now) {
		return lockedUntil != null && lockedUntil.isAfter(now);
	}

	/** @return true if this failure locked the account */
	public boolean recordFailedLogin(LocalDateTime now, int maxAttempts, int lockMinutes) {
		failedAttempts++;
		if (failedAttempts >= maxAttempts) {
			lockedUntil = now.plusMinutes(lockMinutes);
			failedAttempts = 0;
			return true;
		}
		return false;
	}

	public void recordSuccessfulLogin(LocalDateTime now) {
		unlock();
		lastLoginAt = now;
	}

	public void unlock() {
		failedAttempts = 0;
		lockedUntil = null;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getFullName() {
		return fullName;
	}

	public Role getRole() {
		return role;
	}

	public Long getDoctorId() {
		return doctorId;
	}

	public Long getHomeBranchId() {
		return homeBranchId;
	}

	public String getMobile() {
		return mobile;
	}

	public String getEmail() {
		return email;
	}

	public boolean isActive() {
		return active;
	}

	public boolean isMustChangePassword() {
		return mustChangePassword;
	}

	public int getFailedAttempts() {
		return failedAttempts;
	}

	public LocalDateTime getLockedUntil() {
		return lockedUntil;
	}

	public LocalDateTime getLastLoginAt() {
		return lastLoginAt;
	}

}
