package com.vihan.hospital.user;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vihan.hospital.audit.AuditAction;
import com.vihan.hospital.audit.AuditService;
import com.vihan.hospital.branch.BranchRepository;
import com.vihan.hospital.common.error.ApiException.BadRequestException;
import com.vihan.hospital.common.error.ApiException.ConflictException;
import com.vihan.hospital.common.error.ApiException.NotFoundException;
import com.vihan.hospital.doctor.Doctor;
import com.vihan.hospital.doctor.DoctorRepository;
import com.vihan.hospital.security.CurrentUser;
import com.vihan.hospital.security.Passwords;
import com.vihan.hospital.security.RefreshTokenService;
import com.vihan.hospital.user.UserDtos.AdminUserDto;
import com.vihan.hospital.user.UserDtos.CreateUserRequest;
import com.vihan.hospital.user.UserDtos.UpdateUserRequest;
import com.vihan.hospital.user.UserDtos.UserWithPasswordDto;

/**
 * Staff accounts (FR-AD1, DESIGN.md §19.6): one account per person, temporary password that must
 * be changed at first login, deactivate instead of delete.
 */
@Service
@Transactional
public class UserAdminService {

	private final AppUserRepository users;
	private final DoctorRepository doctors;
	private final BranchRepository branches;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenService refreshTokens;
	private final AuditService audit;
	private final Clock clock;

	public UserAdminService(AppUserRepository users, DoctorRepository doctors, BranchRepository branches,
			PasswordEncoder passwordEncoder, RefreshTokenService refreshTokens, AuditService audit, Clock clock) {
		this.users = users;
		this.doctors = doctors;
		this.branches = branches;
		this.passwordEncoder = passwordEncoder;
		this.refreshTokens = refreshTokens;
		this.audit = audit;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<AdminUserDto> list() {
		List<AppUser> all = users.findAllByOrderByUsernameAsc();
		List<Long> doctorIds = all.stream().map(AppUser::getDoctorId).filter(Objects::nonNull).toList();
		Map<Long, String> doctorNames = doctors.findAllById(doctorIds).stream()
			.collect(Collectors.toMap(Doctor::getId, Doctor::getFullName));
		return all.stream().map(u -> toDto(u, doctorNames)).toList();
	}

	@Transactional(readOnly = true)
	public AdminUserDto get(Long id) {
		return toDto(find(id));
	}

	public UserWithPasswordDto create(CreateUserRequest req) {
		if (users.existsByUsername(req.username())) {
			throw new ConflictException("USERNAME_EXISTS", "This username is already taken");
		}
		validateRoleLinks(req.role(), req.doctorId(), req.homeBranchId(), null);
		String temporary = Passwords.temporary();
		AppUser user = new AppUser(req.username(), passwordEncoder.encode(temporary));
		user.updateProfile(req.fullName().strip(), req.role(), linkedDoctor(req.role(), req.doctorId()),
				req.homeBranchId(), blankToNull(req.mobile()), blankToNull(req.email()), true);
		users.save(user);
		audit.record(AuditAction.USER_CREATED, "USER", user.getId(), "username=" + user.getUsername() + ", role=" + user.getRole());
		return new UserWithPasswordDto(toDto(user), temporary);
	}

	public AdminUserDto update(Long id, UpdateUserRequest req) {
		AppUser user = find(id);
		boolean self = CurrentUser.id().map(id::equals).orElse(false);
		if (self && (!req.active() || req.role() != user.getRole())) {
			throw new BadRequestException("CANNOT_CHANGE_SELF", "You cannot deactivate your own account or change your own role");
		}
		boolean losesAdmin = user.getRole() == Role.IT_ADMIN && user.isActive()
				&& (req.role() != Role.IT_ADMIN || !req.active());
		if (losesAdmin && users.countByRoleAndActiveTrue(Role.IT_ADMIN) <= 1) {
			throw new ConflictException("LAST_IT_ADMIN", "At least one active IT Admin account is required");
		}
		validateRoleLinks(req.role(), req.doctorId(), req.homeBranchId(), id);

		boolean deactivated = user.isActive() && !req.active();
		user.updateProfile(req.fullName().strip(), req.role(), linkedDoctor(req.role(), req.doctorId()),
				req.homeBranchId(), blankToNull(req.mobile()), blankToNull(req.email()), req.active());
		if (deactivated) {
			refreshTokens.revokeAll(id);   // logged out within one access-token lifetime (30 min)
		}
		audit.record(AuditAction.USER_UPDATED, "USER", id,
				"role=" + req.role() + ", active=" + req.active() + (deactivated ? ", sessions revoked" : ""));
		return toDto(user);
	}

	/** New temporary password; also unlocks the account and ends its sessions (FR-A3). */
	public UserWithPasswordDto resetPassword(Long id) {
		AppUser user = find(id);
		String temporary = Passwords.temporary();
		user.setPassword(passwordEncoder.encode(temporary), true);
		refreshTokens.revokeAll(id);
		audit.record(AuditAction.PASSWORD_RESET, "USER", id, "username=" + user.getUsername());
		return new UserWithPasswordDto(toDto(user), temporary);
	}

	private void validateRoleLinks(Role role, Long doctorId, Long homeBranchId, Long userId) {
		if (role == Role.DOCTOR) {
			if (doctorId == null) {
				throw new BadRequestException("DOCTOR_REQUIRED", "Select the doctor profile for this login");
			}
			if (!doctors.existsById(doctorId)) {
				throw new BadRequestException("UNKNOWN_DOCTOR", "Doctor does not exist");
			}
			boolean taken = userId == null ? users.existsByDoctorId(doctorId) : users.existsByDoctorIdAndIdNot(doctorId, userId);
			if (taken) {
				throw new ConflictException("DOCTOR_HAS_LOGIN", "This doctor already has a login");
			}
		}
		if (role == Role.RECEPTIONIST && homeBranchId == null) {
			throw new BadRequestException("HOME_BRANCH_REQUIRED", "Select the receptionist's home branch");
		}
		if (homeBranchId != null && !branches.existsById(homeBranchId)) {
			throw new BadRequestException("UNKNOWN_BRANCH", "Branch does not exist");
		}
	}

	/** The DB allows a doctor link only for the DOCTOR role, so drop it for other roles. */
	private static Long linkedDoctor(Role role, Long doctorId) {
		return role == Role.DOCTOR ? doctorId : null;
	}

	private AdminUserDto toDto(AppUser u) {
		Map<Long, String> names = u.getDoctorId() == null ? Map.of()
				: doctors.findById(u.getDoctorId()).stream().collect(Collectors.toMap(Doctor::getId, Doctor::getFullName));
		return toDto(u, names);
	}

	private AdminUserDto toDto(AppUser u, Map<Long, String> doctorNames) {
		return new AdminUserDto(u.getId(), u.getUsername(), u.getFullName(), u.getRole(), u.getDoctorId(),
				u.getDoctorId() == null ? null : doctorNames.get(u.getDoctorId()), u.getHomeBranchId(), u.getMobile(), u.getEmail(), u.isActive(),
				u.isMustChangePassword(), u.isLocked(LocalDateTime.now(clock)), u.getLastLoginAt());
	}

	private AppUser find(Long id) {
		return users.findById(id).orElseThrow(() -> new NotFoundException("User", id));
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.strip();
	}

}
