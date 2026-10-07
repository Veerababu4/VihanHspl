package com.vihan.hospital.visit;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRepository extends JpaRepository<Visit, Long> {

	Optional<Visit> findByAppointmentId(Long appointmentId);

	/** The patient's visit list, newest first (FR-PT4). */
	List<Visit> findByPatientIdOrderByVisitDateDescIdDesc(Long patientId);

}
