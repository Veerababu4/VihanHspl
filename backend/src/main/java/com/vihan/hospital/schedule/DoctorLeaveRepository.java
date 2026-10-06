package com.vihan.hospital.schedule;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DoctorLeaveRepository extends JpaRepository<DoctorLeave, Long> {

	/** Leaves of a doctor that touch [from, to]. */
	@Query("""
			select l from DoctorLeave l
			where l.doctorId = :doctorId and l.fromDate <= :to and l.toDate >= :from
			order by l.fromDate""")
	List<DoctorLeave> findOverlapping(@Param("doctorId") Long doctorId, @Param("from") LocalDate from,
			@Param("to") LocalDate to);

	/** Current and upcoming leaves (past ones are kept but not listed). */
	List<DoctorLeave> findByDoctorIdAndToDateGreaterThanEqualOrderByFromDate(Long doctorId, LocalDate from);

}
