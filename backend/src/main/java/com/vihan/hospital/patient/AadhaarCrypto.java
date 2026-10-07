package com.vihan.hospital.patient;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Aadhaar at rest (DESIGN.md §10.7, §19.1): AES-256-GCM for the stored number (random 12-byte IV
 * per value, stored as Base64 of IV + ciphertext) and HMAC-SHA256 with a separate key for exact
 * search and uniqueness. Both keys come from the environment; the app refuses to start without
 * them, so a misconfigured server can't silently store unprotected data.
 *
 * <p><b>Keep the keys safe:</b> losing {@code AADHAAR_ENC_KEY} makes stored numbers unreadable,
 * and changing {@code AADHAAR_HASH_KEY} breaks search and duplicate checks for existing patients.
 */
@Component
public class AadhaarCrypto {

	/** {@code app.aadhaar.*}: Base64 of 32 random bytes each. */
	@ConfigurationProperties("app.aadhaar")
	public record Keys(String encKey, String hashKey) {
	}

	private static final int IV_BYTES = 12;
	private static final int TAG_BITS = 128;

	private final SecretKeySpec encKey;
	private final SecretKeySpec hashKey;
	private final SecureRandom random = new SecureRandom();

	public AadhaarCrypto(Keys keys) {
		this.encKey = new SecretKeySpec(key(keys.encKey(), "AADHAAR_ENC_KEY"), "AES");
		this.hashKey = new SecretKeySpec(key(keys.hashKey(), "AADHAAR_HASH_KEY"), "HmacSHA256");
	}

	public String encrypt(String aadhaar) {
		try {
			byte[] iv = new byte[IV_BYTES];
			random.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, encKey, new GCMParameterSpec(TAG_BITS, iv));
			byte[] ct = cipher.doFinal(aadhaar.getBytes(StandardCharsets.US_ASCII));
			return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array());
		}
		catch (GeneralSecurityException e) {
			throw new IllegalStateException("Aadhaar encryption failed", e);
		}
	}

	/** Not exposed by any API in Phase 1 (only the last 4 digits are shown); kept for key rotation / audits. */
	public String decrypt(String stored) {
		try {
			byte[] all = Base64.getDecoder().decode(stored);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, encKey, new GCMParameterSpec(TAG_BITS, all, 0, IV_BYTES));
			return new String(cipher.doFinal(all, IV_BYTES, all.length - IV_BYTES), StandardCharsets.US_ASCII);
		}
		catch (GeneralSecurityException e) {
			throw new IllegalStateException("Aadhaar decryption failed", e);
		}
	}

	/** Same number → same hash (64 hex characters), so it can be searched and kept unique. */
	public String hash(String aadhaar) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(hashKey);
			return HexFormat.of().formatHex(mac.doFinal(aadhaar.getBytes(StandardCharsets.US_ASCII)));
		}
		catch (GeneralSecurityException e) {
			throw new IllegalStateException("Aadhaar hashing failed", e);
		}
	}

	private static byte[] key(String base64, String envName) {
		byte[] key;
		try {
			key = base64 == null ? new byte[0] : Base64.getDecoder().decode(base64.strip());
		}
		catch (IllegalArgumentException e) {
			throw new IllegalStateException(envName + " is not valid Base64");
		}
		if (key.length != 32) {
			throw new IllegalStateException(envName + " must be Base64 of 32 random bytes (see .env.example)");
		}
		return key;
	}

}
