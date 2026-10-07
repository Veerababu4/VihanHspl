package com.vihan.hospital.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AadhaarTest {

	private static final String KEY_A = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
	private static final String KEY_B = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";

	/** A fictional, checksum-valid number. */
	private static final String VALID = "234123412346";

	@Test
	void verhoeffMatchesTheStandardExample() {
		assertThat(Aadhaar.verhoeffCheckDigit("236")).isEqualTo(3);   // the textbook example: 236 → 2363
		assertThat(Aadhaar.verhoeffValid("2363")).isTrue();
		assertThat(Aadhaar.verhoeffValid("2364")).isFalse();
	}

	@Test
	void acceptsValidNumbersWithSpacesOrDashes() {
		assertThat(Aadhaar.verhoeffValid(VALID)).isTrue();
		assertThat(Aadhaar.normalize("2341 2341 2346")).isEqualTo(VALID);
		assertThat(Aadhaar.normalize("2341-2341-2346")).isEqualTo(VALID);
	}

	@ParameterizedTest
	@ValueSource(strings = { "234123412345", "134123412346", "034123412346", "23412341234", "2341234123467", "23412341234x", "" })
	void rejectsWrongChecksumLengthOrFirstDigit(String input) {
		assertThat(Aadhaar.normalize(input)).isNull();
	}

	@Test
	void everyGeneratedSampleNumberIsValid() {
		Random r = new Random(1);
		for (int i = 0; i < 500; i++) {
			assertThat(Aadhaar.normalize(SamplePatientSeeder.fictionalAadhaar(r))).isNotNull();
		}
	}

	@Test
	void encryptionRoundTripsWithAFreshIvEachTime() {
		AadhaarCrypto crypto = new AadhaarCrypto(new AadhaarCrypto.Keys(KEY_A, KEY_B));
		String first = crypto.encrypt(VALID);
		String second = crypto.encrypt(VALID);
		assertThat(first).isNotEqualTo(second).doesNotContain(VALID);
		assertThat(crypto.decrypt(first)).isEqualTo(VALID);
		assertThat(crypto.decrypt(second)).isEqualTo(VALID);
		assertThat(first.length()).isLessThanOrEqualTo(200);   // fits aadhaar_enc
	}

	@Test
	void hashIsStableAndDependsOnTheKey() {
		AadhaarCrypto crypto = new AadhaarCrypto(new AadhaarCrypto.Keys(KEY_A, KEY_B));
		AadhaarCrypto otherKey = new AadhaarCrypto(new AadhaarCrypto.Keys(KEY_A, KEY_A));
		assertThat(crypto.hash(VALID)).hasSize(64).isEqualTo(crypto.hash(VALID)).isNotEqualTo(otherKey.hash(VALID));
	}

	@Test
	void tamperedCiphertextIsRejected() {
		AadhaarCrypto crypto = new AadhaarCrypto(new AadhaarCrypto.Keys(KEY_A, KEY_B));
		char[] c = crypto.encrypt(VALID).toCharArray();
		c[20] = c[20] == 'A' ? 'B' : 'A';
		assertThatThrownBy(() -> crypto.decrypt(new String(c))).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void missingOrShortKeysStopTheApp() {
		assertThatThrownBy(() -> new AadhaarCrypto(new AadhaarCrypto.Keys("", KEY_B)))
			.hasMessageContaining("AADHAAR_ENC_KEY");
		assertThatThrownBy(() -> new AadhaarCrypto(new AadhaarCrypto.Keys(KEY_A, "c2hvcnQ=")))
			.hasMessageContaining("AADHAAR_HASH_KEY");
	}

}
