package com.vihan.hospital.common.web;

import java.util.List;

import org.springframework.data.domain.Page;

/** Stable JSON shape for paginated lists (Spring's Page JSON is not a stable contract). */
public record PageDto<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

	public static <T> PageDto<T> from(Page<T> page) {
		return new PageDto<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
				page.getTotalPages());
	}

}
