package com.vihan.hospital.schedule;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

	/** Active templates for a doctor that are in effect on the given date. */
	@Query("""
			select s from DoctorSchedule s
			where s.doctorId = :doctorId and s.active = true
			  and s.effectiveFrom <= :date and (s.effectiveTo is null or s.effectiveTo >= :date)
			order by s.dayOfWeek, s.startTime""")
	List<DoctorSchedule> findInEffect(@Param("doctorId") Long doctorId, @Param("date") LocalDate date);

	/** All active templates of a doctor (any effective dates), for slot planning and the editor. */
	List<DoctorSchedule> findByDoctorIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(Long doctorId);

	@Query("select distinct s.doctorId from DoctorSchedule s where s.active = true")
	List<Long> findDoctorIdsWithActiveSchedules();

}
