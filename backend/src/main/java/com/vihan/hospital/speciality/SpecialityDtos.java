package com.vihan.hospital.speciality;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class SpecialityDtos {

	private SpecialityDtos() {
	}

	/** Public view. */
	public record SpecialityDto(Long id, String name, String description, String iconUrl) {
		public static SpecialityDto from(Speciality s) {
			return new SpecialityDto(s.getId(), s.getName(), s.getDescription(), s.getIconUrl());
		}
	}

	/** Admin view: includes inactive specialities. */
	public record AdminSpecialityDto(Long id, String name, String description, boolean active) {
		public static AdminSpecialityDto from(Speciality s) {
			return new AdminSpecialityDto(s.getId(), s.getName(), s.getDescription(), s.isActive());
		}
	}

	public record SpecialityRequest(
			@NotBlank @Size(max = 80) String name,
			@Size(max = 1000) String description,
			@NotNull Boolean active) {
	}

}
