package com.vihan.hospital.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import com.vihan.hospital.common.error.ApiException;

class PasswordsTest {

	@Test
	void policyNeedsEightCharactersWithLettersAndDigits() {
		assertThatCode(() -> Passwords.checkPolicy("abcdef12")).doesNotThrowAnyException();
		assertThatThrownBy(() -> Passwords.checkPolicy("abc12")).isInstanceOf(ApiException.class);
		assertThatThrownBy(() -> Passwords.checkPolicy("abcdefgh")).isInstanceOf(ApiException.class);
		assertThatThrownBy(() -> Passwords.checkPolicy("12345678")).isInstanceOf(ApiException.class);
		assertThatThrownBy(() -> Passwords.checkPolicy("a1".repeat(40))).isInstanceOf(ApiException.class);   // > 72 bytes
	}

	@RepeatedTest(20)
	void temporaryPasswordsPassThePolicyAndAvoidLookAlikes() {
		String p = Passwords.temporary();
		assertThat(p).hasSize(12).doesNotContainAnyWhitespaces().matches("[^0O1lI]+");
		assertThatCode(() -> Passwords.checkPolicy(p)).doesNotThrowAnyException();
	}

	@Test
	void temporaryPasswordsAreRandom() {
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < 1000; i++) {
			seen.add(Passwords.temporary());
		}
		assertThat(seen).hasSize(1000);
	}

}
