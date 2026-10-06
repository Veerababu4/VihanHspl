package com.vihan.hospital.branch;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vihan.hospital.branch.BranchAdminDtos.AdminBranchDto;
import com.vihan.hospital.branch.BranchAdminDtos.BranchRequest;
import com.vihan.hospital.branch.BranchService.BranchDetailDto;

public final class BranchControllers {

	private BranchControllers() {
	}

	@RestController
	@RequestMapping("/api/public/branches")
	public static class PublicBranchController {

		private final BranchService service;

		public PublicBranchController(BranchService service) {
			this.service = service;
		}

		@GetMapping
		public List<BranchDto> list() {
			return service.listActive();
		}

		@GetMapping("/{code}")
		public BranchDetailDto detail(@PathVariable String code) {
			return service.detail(code);
		}

	}

	/** Deactivating (active=false) is the "delete": branches are master data (DESIGN.md §9.1). */
	@RestController
	@RequestMapping("/api/admin/branches")
	@PreAuthorize("hasRole('IT_ADMIN')")
	public static class AdminBranchController {

		private final BranchService service;

		public AdminBranchController(BranchService service) {
			this.service = service;
		}

		@GetMapping
		public List<AdminBranchDto> list() {
			return service.listAll();
		}

		@PostMapping
		@ResponseStatus(HttpStatus.CREATED)
		public AdminBranchDto create(@Valid @RequestBody BranchRequest req) {
			return service.create(req);
		}

		@PutMapping("/{id}")
		public AdminBranchDto update(@PathVariable Long id, @Valid @RequestBody BranchRequest req) {
			return service.update(id, req);
		}

		@PostMapping(path = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
		public AdminBranchDto uploadImage(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
			return service.uploadImage(id, file);
		}

	}

}
