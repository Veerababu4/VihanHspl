package com.vihan.hospital.branch;

import com.vihan.hospital.storage.FileUrls;

/** Public view of a branch. Entities are never returned from controllers (DESIGN.md §8). */
public record BranchDto(Long id, String code, String name, String address, String city, String district,
		String pincode, String phone, String emergencyPhone, String email, String mapUrl, String imageUrl,
		String openingHours) {

	public static BranchDto from(Branch b, FileUrls urls) {
		return new BranchDto(b.getId(), b.getCode(), b.getName(), b.getAddress(), b.getCity(), b.getDistrict(),
				b.getPincode(), b.getPhone(), b.getEmergencyPhone(), b.getEmail(), b.getMapUrl(),
				urls.toUrl(b.getImageUrl()), b.getOpeningHours());
	}

}
