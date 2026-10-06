package com.vihan.hospital.speciality;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.vihan.hospital.common.entity.AuditableEntity;

@Entity
@Table(name = "speciality")
public class Speciality extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "speciality_id")
	private Long id;

	@Column(nullable = false, length = 80)
	private String name;

	@Column(length = 1000)
	private String description;

	@Column(name = "icon_url", length = 500)
	private String iconUrl;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	protected Speciality() {
	}

	public Speciality(String name, String description, boolean active) {
		update(name, description, active);
	}

	public void update(String name, String description, boolean active) {
		this.name = name.strip();
		this.description = description;
		this.active = active;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getIconUrl() {
		return iconUrl;
	}

	public boolean isActive() {
		return active;
	}

}
