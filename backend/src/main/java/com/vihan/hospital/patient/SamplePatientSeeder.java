package com.vihan.hospital.patient;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * LOCAL PROFILE ONLY: about 40 <b>fictional</b> patients (DESIGN.md §9.4) with checksum-valid
 * fictional Aadhaar numbers, encrypted with the local key like real ones. The numbers come from a
 * fixed random seed, so each sample patient is recognised by its Aadhaar hash and created only
 * once; patients registered by hand are never touched. Every third pair shares a mobile number
 * (a family), to show the "same mobile, different person" case.
 */
@Component
@Profile("local")
@Order(3)
public class SamplePatientSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(SamplePatientSeeder.class);
	private static final int COUNT = 40;

	private static final List<String> MEN = List.of("Ravi", "Srinivas", "Venkatesh", "Naresh", "Mahesh", "Suresh",
			"Ramesh", "Prasad", "Krishna", "Anil", "Kiran", "Raju", "Sai Teja", "Nagaraju", "Shiva", "Harish");
	private static final List<String> WOMEN = List.of("Lakshmi", "Sravani", "Padma", "Swathi", "Anitha", "Kavitha",
			"Sunitha", "Bhavani", "Divya", "Manasa", "Sirisha", "Jyothi", "Renuka", "Saritha", "Madhavi", "Haritha");
	private static final List<String> SURNAMES = List.of("Reddy", "Goud", "Rao", "Naidu", "Yadav", "Chary", "Kumar",
			"Sharma", "Kandula", "Bandi", "Gaddam", "Pasupuleti", "Vemula", "Thota", "Mekala", "Bollam");
	private static final List<String> CITIES = List.of("Miryalaguda", "Huzurnagar", "Kodad", "Nalgonda", "Suryapet");
	private static final List<String> BLOOD_GROUPS = List.of("O+", "B+", "A+", "AB+", "O-", "B-", "A-");

	private final PatientRepository patients;
	private final AadhaarCrypto crypto;
	private final JdbcTemplate jdbc;
	private final TransactionTemplate tx;
	private final Clock clock;

	public SamplePatientSeeder(PatientRepository patients, AadhaarCrypto crypto, JdbcTemplate jdbc, TransactionTemplate tx,
			Clock clock) {
		this.patients = patients;
		this.crypto = crypto;
		this.jdbc = jdbc;
		this.tx = tx;
		this.clock = clock;
	}

	@Override
	public void run(ApplicationArguments args) {
		Integer created = tx.execute(status -> seed());
		if (created != null && created > 0) {
			log.info("Created {} fictional sample patients", created);
		}
	}

	private int seed() {
		Random r = new Random(2026);   // fixed: the same 40 people every time
		List<Long> branchIds = jdbc.queryForList("SELECT branch_id FROM branch ORDER BY branch_id", Long.class);
		LocalDate today = LocalDate.now(clock);
		LocalDateTime now = LocalDateTime.now(clock);
		String familyMobile = null;
		int created = 0;
		for (int i = 0; i < COUNT; i++) {
			// Draw every value in the same order on every run, created or not, so the people stay the same.
			boolean female = r.nextBoolean();
			String first = (female ? WOMEN : MEN).get(r.nextInt(16));
			String surname = SURNAMES.get(r.nextInt(SURNAMES.size()));
			int age = 1 + r.nextInt(85);
			String ownMobile = "9" + String.format("%09d", r.nextInt(1_000_000_000));
			String aadhaar = fictionalAadhaar(r);
			String city = CITIES.get(r.nextInt(CITIES.size()));
			String blood = BLOOD_GROUPS.get(r.nextInt(BLOOD_GROUPS.size()));
			long branchId = branchIds.get(r.nextInt(branchIds.size()));
			boolean withContact = r.nextInt(3) > 0;

			// Pairs 0-1, 6-7, 12-13 ... are families sharing one phone.
			String mobile = i % 6 == 1 ? familyMobile : ownMobile;
			familyMobile = ownMobile;
			String hash = crypto.hash(aadhaar);
			if (patients.findByAadhaarHash(hash).isPresent()) {
				continue;
			}
			String uhid = "VMH-%02d-%06d".formatted(today.getYear() % 100, patients.nextUhidNumber());
			Patient p = new Patient(uhid, first + " " + surname, PatientService.estimatedDob(age, today), true,
					female ? "F" : "M", mobile, branchId, now);
			if (withContact) {
				p.updateContact(null, (10 + i) + "-" + (100 + i) + ", Sample Colony", city, blood,
						(female ? "Sample Husband " : "Sample Wife ") + surname, ownMobile.substring(0, 9) + "1");
			}
			p.setAadhaar(crypto.encrypt(aadhaar), hash, aadhaar.substring(8));
			patients.save(p);
			created++;
		}
		return created;
	}

	/** 11 random digits (first one 2-9) plus the Verhoeff check digit. */
	static String fictionalAadhaar(Random r) {
		StringBuilder sb = new StringBuilder().append(2 + r.nextInt(8));
		for (int i = 0; i < 10; i++) {
			sb.append(r.nextInt(10));
		}
		return sb.append(Aadhaar.verhoeffCheckDigit(sb.toString())).toString();
	}

}
