package com.vihan.hospital.user;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vihan.hospital.user.UserDtos.AdminUserDto;
import com.vihan.hospital.user.UserDtos.CreateUserRequest;
import com.vihan.hospital.user.UserDtos.UpdateUserRequest;
import com.vihan.hospital.user.UserDtos.UserWithPasswordDto;

/** Staff accounts. "Delete" = update with active=false (DESIGN.md §19.6). */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('IT_ADMIN')")
public class AdminUserController {

	private final UserAdminService service;

	public AdminUserController(UserAdminService service) {
		this.service = service;
	}

	@GetMapping
	public List<AdminUserDto> list() {
		return service.list();
	}

	@GetMapping("/{id}")
	public AdminUserDto get(@PathVariable Long id) {
		return service.get(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UserWithPasswordDto create(@Valid @RequestBody CreateUserRequest req) {
		return service.create(req);
	}

	@PutMapping("/{id}")
	public AdminUserDto update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
		return service.update(id, req);
	}

	@PostMapping("/{id}/reset-password")
	public UserWithPasswordDto resetPassword(@PathVariable Long id) {
		return service.resetPassword(id);
	}

}
