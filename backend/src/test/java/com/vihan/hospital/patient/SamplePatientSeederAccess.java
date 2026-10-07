package com.vihan.hospital.patient;

import java.util.Random;

/** Lets tests in other packages make fictional, checksum-valid Aadhaar numbers. */
public final class SamplePatientSeederAccess {

	private SamplePatientSeederAccess() {
	}

	public static String fictionalAadhaar(Random random) {
		return SamplePatientSeeder.fictionalAadhaar(random);
	}

}
