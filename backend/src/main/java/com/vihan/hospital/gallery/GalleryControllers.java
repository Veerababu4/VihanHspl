package com.vihan.hospital.gallery;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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

import com.vihan.hospital.gallery.GalleryDtos.AdminGalleryImageDto;
import com.vihan.hospital.gallery.GalleryDtos.GalleryImageDto;
import com.vihan.hospital.gallery.GalleryDtos.GalleryImageRequest;

public final class GalleryControllers {

	private GalleryControllers() {
	}

	@RestController
	@RequestMapping("/api/public/gallery")
	public static class PublicGalleryController {

		private final GalleryService service;

		public PublicGalleryController(GalleryService service) {
			this.service = service;
		}

		@GetMapping
		public List<GalleryImageDto> list(@RequestParam(required = false) String branch) {
			return service.listPublic(branch);
		}

	}

	@RestController
	@RequestMapping("/api/admin/gallery")
	@PreAuthorize("hasRole('IT_ADMIN')")
	public static class AdminGalleryController {

		private final GalleryService service;

		public AdminGalleryController(GalleryService service) {
			this.service = service;
		}

		@GetMapping
		public List<AdminGalleryImageDto> list() {
			return service.listAdmin();
		}

		/** Multipart: the image in {@code file}, metadata as form fields. */
		@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
		@ResponseStatus(HttpStatus.CREATED)
		public AdminGalleryImageDto upload(@RequestPart("file") MultipartFile file,
				@Valid @ModelAttribute GalleryImageRequest details) {
			return service.upload(file, details);
		}

		@PutMapping("/{id}")
		public AdminGalleryImageDto update(@PathVariable Long id, @Valid @RequestBody GalleryImageRequest details) {
			return service.update(id, details);
		}

		@DeleteMapping("/{id}")
		@ResponseStatus(HttpStatus.NO_CONTENT)
		public void delete(@PathVariable Long id) {
			service.delete(id);
		}

	}

}
