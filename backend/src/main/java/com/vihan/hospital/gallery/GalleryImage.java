package com.vihan.hospital.gallery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.vihan.hospital.common.entity.AuditableEntity;

@Entity
@Table(name = "gallery_image")
public class GalleryImage extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "image_id")
	private Long id;

	/** null = network-wide image, not tied to one branch. */
	@Column(name = "branch_id")
	private Long branchId;

	@Column(length = 120)
	private String title;

	@Column(length = 500)
	private String caption;

	@Column(name = "image_url", nullable = false, length = 500)
	private String imageUrl;

	@Column(name = "thumb_url", nullable = false, length = 500)
	private String thumbUrl;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	protected GalleryImage() {
	}

	public GalleryImage(String imageUrl, String thumbUrl) {
		this.imageUrl = imageUrl;
		this.thumbUrl = thumbUrl;
	}

	public void updateDetails(Long branchId, String title, String caption, int sortOrder, boolean active) {
		this.branchId = branchId;
		this.title = title;
		this.caption = caption;
		this.sortOrder = sortOrder;
		this.active = active;
	}

	public Long getId() {
		return id;
	}

	public Long getBranchId() {
		return branchId;
	}

	public String getTitle() {
		return title;
	}

	public String getCaption() {
		return caption;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getThumbUrl() {
		return thumbUrl;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public boolean isActive() {
		return active;
	}

}
