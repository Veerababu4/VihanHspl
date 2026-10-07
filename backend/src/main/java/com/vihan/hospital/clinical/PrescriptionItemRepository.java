package com.vihan.hospital.clinical;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {

	List<PrescriptionItem> findByVisitIdOrderBySortOrder(Long visitId);

	List<PrescriptionItem> findByVisitIdInOrderBySortOrder(Collection<Long> visitIds);

	/** Saving replaces the whole prescription (FR-C2). */
	@Modifying(flushAutomatically = true)
	@Query("delete from PrescriptionItem p where p.visitId = :visitId")
	int deleteByVisit(@Param("visitId") Long visitId);

}
