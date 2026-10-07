package com.vihan.hospital.patient;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** FR-PT2: which names count as "possibly the same person" when the mobile is the same. */
class NameMatcherTest {

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"Ravi Kumar          | RAVI KUMAR",
			"Ravi Kumar          | Kumar Ravi",
			"K. Ravi Kumar       | Ravi Kumar Kandula",
			"Smt Lakshmi Devi    | Lakshmi  Devi",
			"Ravi                | Ravi Kumar",
			"Dr. Sita Rani       | sita rani",
			"B/O Lakshmi         | Baby of Lakshmi" })
	void similar(String a, String b) {
		assertThat(NameMatcher.similar(a, b)).isTrue();
		assertThat(NameMatcher.similar(b, a)).isTrue();
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"Ravi Kumar          | Sita Kumar",          // family sharing a phone
			"Ravi Kumar          | Ravi Teja",
			"P. Ravi Kumar       | Ravi Kumar Kandula",  // the initial fits no word
			"Mr.                 | Mr." })
	void notSimilar(String a, String b) {
		assertThat(NameMatcher.similar(a, b)).isFalse();
	}

}
