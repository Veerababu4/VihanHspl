package com.vihan.hospital.speciality;

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

import com.vihan.hospital.common.ReferenceDataCache;
import com.vihan.hospital.speciality.SpecialityDtos.AdminSpecialityDto;
import com.vihan.hospital.speciality.SpecialityDtos.SpecialityDto;
import com.vihan.hospital.speciality.SpecialityDtos.SpecialityRequest;

public final class SpecialityControllers {

	private SpecialityControllers() {
	}

	@RestController
	@RequestMapping("/api/public/specialities")
	public static class PublicSpecialityController {

		private final ReferenceDataCache cache;

		public PublicSpecialityController(ReferenceDataCache cache) {
			this.cache = cache;
		}

		@GetMapping
		public List<SpecialityDto> list() {
			return cache.specialities();
		}

	}

	/** Deactivating (active=false) is the "delete": specialities are master data (DESIGN.md §9.1). */
	@RestController
	@RequestMapping("/api/admin/specialities")
	@PreAuthorize("hasRole('IT_ADMIN')")
	public static class AdminSpecialityController {

		private final SpecialityAdminService service;

		public AdminSpecialityController(SpecialityAdminService service) {
			this.service = service;
		}

		@GetMapping
		public List<AdminSpecialityDto> list() {
			return service.list();
		}

		@PostMapping
		@ResponseStatus(HttpStatus.CREATED)
		public AdminSpecialityDto create(@Valid @RequestBody SpecialityRequest req) {
			return service.create(req);
		}

		@PutMapping("/{id}")
		public AdminSpecialityDto update(@PathVariable Long id, @Valid @RequestBody SpecialityRequest req) {
			return service.update(id, req);
		}

	}

}
