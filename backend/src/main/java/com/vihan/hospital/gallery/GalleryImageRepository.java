package com.vihan.hospital.gallery;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GalleryImageRepository extends JpaRepository<GalleryImage, Long> {

	List<GalleryImage> findByActiveTrueOrderBySortOrderAscIdAsc();

	List<GalleryImage> findByActiveTrueAndBranchIdOrderBySortOrderAscIdAsc(Long branchId);

	List<GalleryImage> findAllByOrderBySortOrderAscIdAsc();

}
