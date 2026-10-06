package com.vihan.hospital.speciality;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialityRepository extends JpaRepository<Speciality, Long> {

	List<Speciality> findByActiveTrueOrderByNameAsc();

	List<Speciality> findAllByOrderByNameAsc();

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

}
