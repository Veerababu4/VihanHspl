package com.vihan.hospital.user;

import java.time.Instant;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserDtos {

	private UserDtos() {
	}

	// ---- auth ----

	public record LoginRequest(@NotBlank @Size(max = 60) String username, @NotBlank @Size(max = 200) String password) {
	}

	public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {
	}

	/** The logged-in user, as the staff portal needs it. */
	public record MeDto(Long id, String username, String fullName, Role role, Long doctorId, Long homeBranchId,
			boolean mustChangePassword) {

		public static MeDto from(AppUser u) {
			return new MeDto(u.getId(), u.getUsername(), u.getFullName(), u.getRole(), u.getDoctorId(),
					u.getHomeBranchId(), u.isMustChangePassword());
		}
	}

	/** Login / refresh / change-password response. The refresh token travels only in the httpOnly cookie. */
	public record SessionDto(String accessToken, Instant expiresAt, MeDto user) {
	}

	// ---- IT Admin user management (FR-AD1) ----

	public record AdminUserDto(Long id, String username, String fullName, Role role, Long doctorId, String doctorName,
			Long homeBranchId, String mobile, String email, boolean active, boolean mustChangePassword, boolean locked,
			LocalDateTime lastLoginAt) {
	}

	/** Returned once when an account is created or a password is reset; never stored in plain text. */
	public record UserWithPasswordDto(AdminUserDto user, String temporaryPassword) {
	}

	public record CreateUserRequest(
			@NotBlank @Pattern(regexp = "[a-z0-9._-]{3,60}", message = "3-60 lowercase letters, digits, . _ -") String username,
			@NotBlank @Size(max = 120) String fullName,
			@NotNull Role role,
			Long doctorId,
			Long homeBranchId,
			@Pattern(regexp = "[6-9]\\d{9}", message = "must be a 10-digit Indian mobile number") String mobile,
			@Email @Size(max = 120) String email) {
	}

	public record UpdateUserRequest(
			@NotBlank @Size(max = 120) String fullName,
			@NotNull Role role,
			Long doctorId,
			Long homeBranchId,
			@Pattern(regexp = "[6-9]\\d{9}", message = "must be a 10-digit Indian mobile number") String mobile,
			@Email @Size(max = 120) String email,
			@NotNull Boolean active) {
	}

}
