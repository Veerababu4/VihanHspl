package com.vihan.hospital.common.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Every error type maps to the single ApiError shape; no Spring context or database needed. */
class GlobalExceptionHandlerTest {

	record BookingForm(@NotBlank String name, @Pattern(regexp = "[6-9]\\d{9}", message = "Invalid mobile") String mobile) {
	}

	@RestController
	static class ThrowingController {

		@GetMapping("/not-found")
		void notFound() {
			throw new ApiException.NotFoundException("Doctor", 42);
		}

		@GetMapping("/conflict")
		void conflict() {
			throw new ApiException.ConflictException("SLOT_TAKEN", "Slot no longer available");
		}

		@GetMapping("/stale")
		void stale() {
			throw new ObjectOptimisticLockingFailureException("Slot", 1L);
		}

		@GetMapping("/boom")
		void boom() {
			throw new IllegalStateException("secret internal detail");
		}

		@PostMapping("/book")
		void book(@Valid @RequestBody BookingForm form) {
		}

	}

	MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void notFound() throws Exception {
		mvc.perform(get("/not-found"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("Doctor not found: 42"))
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void businessConflictKeepsItsCode() throws Exception {
		mvc.perform(get("/conflict"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SLOT_TAKEN"));
	}

	@Test
	void optimisticLockIsConflict() throws Exception {
		mvc.perform(get("/stale"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("CONCURRENT_UPDATE"));
	}

	@Test
	void unexpectedErrorHidesInternalDetails() throws Exception {
		mvc.perform(get("/boom"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
			.andExpect(jsonPath("$.message").value("Something went wrong. Please try again."));
	}

	@Test
	void validationListsFieldErrors() throws Exception {
		mvc.perform(post("/book").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"mobile\":\"12345\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.fieldErrors.length()").value(2));
	}

	@Test
	void malformedJsonIsBadRequest() throws Exception {
		mvc.perform(post("/book").contentType(MediaType.APPLICATION_JSON).content("{not json"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("BAD_REQUEST"));
	}

}
