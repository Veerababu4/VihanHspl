package com.vihan.hospital.branch;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {

	List<Branch> findByActiveTrueOrderByIdAsc();

}
