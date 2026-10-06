package com.vihan.hospital.audit;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One audit entry. Append-only: there is no update or delete API. */
@Entity
@Table(name = "audit_log")
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "audit_id")
	private Long id;

	/** null = public / not logged in (e.g. failed login with an unknown username). */
	@Column(name = "user_id")
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private AuditAction action;

	@Column(name = "entity_type", length = 40)
	private String entityType;

	@Column(name = "entity_id", length = 40)
	private String entityId;

	/** Never contains Aadhaar, clinical text or passwords. */
	@Column(length = 1000)
	private String details;

	@Column(name = "ip_address", length = 45)
	private String ipAddress;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	protected AuditLog() {
	}

	public AuditLog(Long userId, AuditAction action, String entityType, String entityId, String details,
			String ipAddress, LocalDateTime createdAt) {
		this.userId = userId;
		this.action = action;
		this.entityType = entityType;
		this.entityId = entityId;
		this.details = details;
		this.ipAddress = ipAddress;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public AuditAction getAction() {
		return action;
	}

	public String getEntityType() {
		return entityType;
	}

	public String getEntityId() {
		return entityId;
	}

	public String getDetails() {
		return details;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

}
