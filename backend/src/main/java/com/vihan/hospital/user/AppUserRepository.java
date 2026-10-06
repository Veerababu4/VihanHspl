package com.vihan.hospital.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	Optional<AppUser> findByUsername(String username);

	boolean existsByUsername(String username);

	List<AppUser> findAllByOrderByUsernameAsc();

	long countByRoleAndActiveTrue(Role role);

	boolean existsByDoctorIdAndIdNot(Long doctorId, Long id);

	boolean existsByDoctorId(Long doctorId);

}
