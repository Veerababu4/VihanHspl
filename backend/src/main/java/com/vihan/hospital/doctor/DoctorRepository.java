package com.vihan.hospital.doctor;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

	/** Speciality and branches are fetched in the same query, so listing doctors is one SQL call. */
	@EntityGraph(attributePaths = { "speciality", "branches" })
	List<Doctor> findByActiveTrue();

	@EntityGraph(attributePaths = { "speciality", "branches" })
	List<Doctor> findAllByOrderByFullNameAsc();

	@EntityGraph(attributePaths = { "speciality", "branches" })
	Optional<Doctor> findWithDetailsById(Long id);

	boolean existsBySpecialityIdAndActiveTrue(Long specialityId);

	boolean existsByRegistrationNoIgnoreCase(String registrationNo);

	boolean existsByRegistrationNoIgnoreCaseAndIdNot(String registrationNo, Long id);

}
