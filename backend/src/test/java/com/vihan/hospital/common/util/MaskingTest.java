package com.vihan.hospital.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MaskingTest {

	@Test
	void masksMiddleSixDigitsOfMobile() {
		assertThat(Masking.mobile("9876543221")).isEqualTo("98XXXXXX21");
	}

	@Test
	void fullyMasksAnythingThatIsNotATenDigitMobile() {
		assertThat(Masking.mobile(null)).isEqualTo("XXXXXXXXXX");
		assertThat(Masking.mobile("98765")).isEqualTo("XXXXXXXXXX");
		assertThat(Masking.mobile("98765abc21")).isEqualTo("XXXXXXXXXX");
	}

	@Test
	void masksAadhaarFromLastFourDigits() {
		assertThat(Masking.aadhaarFromLast4("1234")).isEqualTo("XXXX-XXXX-1234");
		assertThat(Masking.aadhaarFromLast4(null)).isNull();
	}

}
