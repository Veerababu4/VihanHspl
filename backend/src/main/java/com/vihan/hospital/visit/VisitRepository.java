package com.vihan.hospital.visit;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRepository extends JpaRepository<Visit, Long> {

	Optional<Visit> findByAppointmentId(Long appointmentId);

}
