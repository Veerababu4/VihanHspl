package com.vihan.hospital.patient;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** DESIGN.md §10.6: an age alone gives 1 July of the birth year, and the age shown must stay the same. */
class EstimatedDobTest {

	@ParameterizedTest
	@CsvSource({
			"2026-03-01, 30, 1995-07-01",   // before 1 July: this year's birthday hasn't come yet
			"2026-10-07, 30, 1996-07-01",
			"2026-07-01, 30, 1996-07-01",
			"2026-01-15, 0, 2025-07-01",
			"2026-12-31, 0, 2026-07-01" })
	void ageIsKeptOnTheRegistrationDay(LocalDate today, int age, LocalDate expected) {
		LocalDate dob = PatientService.estimatedDob(age, today);
		assertThat(dob).isEqualTo(expected);
		assertThat(Period.between(dob, today).getYears()).isEqualTo(age);
	}

}
