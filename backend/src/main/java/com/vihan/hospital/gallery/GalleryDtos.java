package com.vihan.hospital.gallery;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.vihan.hospital.storage.FileUrls;

public final class GalleryDtos {

	private GalleryDtos() {
	}

	/** @param branchCode null for network-wide images */
	public record GalleryImageDto(Long id, String branchCode, String title, String caption, String imageUrl,
			String thumbUrl) {
	}

	public record AdminGalleryImageDto(Long id, Long branchId, String title, String caption, String imageUrl,
			String thumbUrl, int sortOrder, boolean active) {

		static AdminGalleryImageDto from(GalleryImage g, FileUrls urls) {
			return new AdminGalleryImageDto(g.getId(), g.getBranchId(), g.getTitle(), g.getCaption(),
					urls.toUrl(g.getImageUrl()), urls.toUrl(g.getThumbUrl()), g.getSortOrder(), g.isActive());
		}
	}

	/** Metadata for upload (multipart fields) and edit (JSON). */
	public record GalleryImageRequest(
			Long branchId,
			@Size(max = 120) String title,
			@Size(max = 500) String caption,
			@Min(0) @Max(99999) Integer sortOrder,
			@NotNull Boolean active) {
	}

}
