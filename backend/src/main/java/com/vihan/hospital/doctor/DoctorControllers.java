package com.vihan.hospital.doctor;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vihan.hospital.doctor.DoctorDtos.AdminDoctorDto;
import com.vihan.hospital.doctor.DoctorDtos.DoctorDetailDto;
import com.vihan.hospital.doctor.DoctorDtos.DoctorRequest;
import com.vihan.hospital.doctor.DoctorDtos.DoctorSummaryDto;

public final class DoctorControllers {

	private DoctorControllers() {
	}

	@RestController
	@RequestMapping("/api/public/doctors")
	public static class PublicDoctorController {

		private final DoctorService service;

		public PublicDoctorController(DoctorService service) {
			this.service = service;
		}

		@GetMapping
		public List<DoctorSummaryDto> list(@RequestParam(required = false) String branch,
				@RequestParam(required = false) Long speciality, @RequestParam(required = false) String q) {
			return service.list(branch, speciality, q);
		}

		@GetMapping("/{id}")
		public DoctorDetailDto get(@PathVariable Long id) {
			return service.get(id);
		}

	}

	/** Deactivating (active=false) is the "delete": doctors are master data (DESIGN.md §9.1). */
	@RestController
	@RequestMapping("/api/admin/doctors")
	@PreAuthorize("hasRole('IT_ADMIN')")
	public static class AdminDoctorController {

		private final DoctorAdminService service;

		public AdminDoctorController(DoctorAdminService service) {
			this.service = service;
		}

		@GetMapping
		public List<AdminDoctorDto> list() {
			return service.list();
		}

		@GetMapping("/{id}")
		public AdminDoctorDto get(@PathVariable Long id) {
			return service.get(id);
		}

		@PostMapping
		@ResponseStatus(HttpStatus.CREATED)
		public AdminDoctorDto create(@Valid @RequestBody DoctorRequest req) {
			return service.create(req);
		}

		@PutMapping("/{id}")
		public AdminDoctorDto update(@PathVariable Long id, @Valid @RequestBody DoctorRequest req) {
			return service.update(id, req);
		}

		@PostMapping(path = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
		public AdminDoctorDto uploadPhoto(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
			return service.uploadPhoto(id, file);
		}

		@DeleteMapping("/{id}/photo")
		public AdminDoctorDto removePhoto(@PathVariable Long id) {
			return service.removePhoto(id);
		}

	}

}
