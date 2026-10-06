package com.vihan.hospital.schedule;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchHolidayRepository extends JpaRepository<BranchHoliday, Long> {

	List<BranchHoliday> findByHolidayDateBetweenOrderByHolidayDate(LocalDate from, LocalDate to);

	List<BranchHoliday> findByHolidayDateGreaterThanEqualOrderByHolidayDate(LocalDate from);

	List<BranchHoliday> findByHolidayDate(LocalDate date);

}
