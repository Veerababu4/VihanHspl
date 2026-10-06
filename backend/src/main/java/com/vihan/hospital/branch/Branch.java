package com.vihan.hospital.branch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.vihan.hospital.common.entity.AuditableEntity;

@Entity
@Table(name = "branch")
public class Branch extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "branch_id")
	private Long id;

	@Column(nullable = false, length = 10)
	private String code;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, length = 400)
	private String address;

	@Column(nullable = false, length = 60)
	private String city;

	@Column(length = 60)
	private String district;

	@Column(length = 6)
	private String pincode;

	@Column(length = 15)
	private String phone;

	@Column(name = "emergency_phone", length = 15)
	private String emergencyPhone;

	@Column(length = 120)
	private String email;

	@Column(name = "map_url", length = 500)
	private String mapUrl;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	@Column(name = "opening_hours", length = 200)
	private String openingHours;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	protected Branch() {
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getAddress() {
		return address;
	}

	public String getCity() {
		return city;
	}

	public String getDistrict() {
		return district;
	}

	public String getPincode() {
		return pincode;
	}

	public String getPhone() {
		return phone;
	}

	public String getEmergencyPhone() {
		return emergencyPhone;
	}

	public String getEmail() {
		return email;
	}

	public String getMapUrl() {
		return mapUrl;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getOpeningHours() {
		return openingHours;
	}

	public boolean isActive() {
		return active;
	}

}
