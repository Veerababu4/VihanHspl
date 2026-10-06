package com.vihan.hospital.branch;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.vihan.hospital.storage.FileUrls;

public final class BranchAdminDtos {

	private BranchAdminDtos() {
	}

	public record AdminBranchDto(Long id, String code, String name, String address, String city, String district,
			String pincode, String phone, String emergencyPhone, String email, String mapUrl, String imageUrl,
			String openingHours, boolean active) {

		static AdminBranchDto from(Branch b, FileUrls urls) {
			return new AdminBranchDto(b.getId(), b.getCode(), b.getName(), b.getAddress(), b.getCity(),
					b.getDistrict(), b.getPincode(), b.getPhone(), b.getEmergencyPhone(), b.getEmail(), b.getMapUrl(),
					urls.toUrl(b.getImageUrl()), b.getOpeningHours(), b.isActive());
		}
	}

	/**
	 * @param code   short code used in URLs (e.g. KDD). Fixed after creation.
	 * @param mapUrl must be a Google Maps embed link: the site shows it in an iframe, so other
	 *               sites can't be embedded through this field.
	 */
	public record BranchRequest(
			@NotBlank @Pattern(regexp = "[A-Z]{2,10}", message = "2-10 capital letters") String code,
			@NotBlank @Size(max = 120) String name,
			@NotBlank @Size(max = 400) String address,
			@NotBlank @Size(max = 60) String city,
			@Size(max = 60) String district,
			@Pattern(regexp = "\\d{6}", message = "must be 6 digits") String pincode,
			@Pattern(regexp = "[0-9+\\- ]{6,15}", message = "invalid phone number") String phone,
			@Pattern(regexp = "[0-9+\\- ]{3,15}", message = "invalid phone number") String emergencyPhone,
			@Email @Size(max = 120) String email,
			@Pattern(regexp = "https://(www\\.)?google\\.com/maps\\S*", message = "must be a Google Maps embed link")
			@Size(max = 500) String mapUrl,
			@Size(max = 200) String openingHours,
			@NotNull Boolean active) {
	}

}
