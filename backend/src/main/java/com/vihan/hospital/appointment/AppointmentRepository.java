package com.vihan.hospital.appointment;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

	Optional<Appointment> findByBookingRef(String bookingRef);

	boolean existsByBookingRef(String bookingRef);

	/** Active (booked or checked-in) appointments of a mobile on a date, optionally ignoring one (reschedule). */
	@Query("""
			select count(a) from Appointment a
			where a.patientMobile = :mobile and a.apptDate = :date
			  and a.status in (com.vihan.hospital.appointment.Appointment.Status.BOOKED,
			                   com.vihan.hospital.appointment.Appointment.Status.CHECKED_IN)
			  and (:excludeId is null or a.id <> :excludeId)""")
	long countActiveForMobileOnDate(@Param("mobile") String mobile, @Param("date") LocalDate date,
			@Param("excludeId") Long excludeId);

}
