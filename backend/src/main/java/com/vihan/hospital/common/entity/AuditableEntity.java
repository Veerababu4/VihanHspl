package com.vihan.hospital.common.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Audit columns present on every table (DESIGN.md §9.1). */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@CreatedBy
	@Column(name = "created_by", updatable = false)
	private Long createdBy;

	@LastModifiedDate
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@LastModifiedBy
	@Column(name = "updated_by")
	private Long updatedBy;

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public Long getUpdatedBy() {
		return updatedBy;
	}

}
