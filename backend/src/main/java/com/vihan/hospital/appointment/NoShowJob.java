package com.vihan.hospital.appointment;

import java.time.Clock;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Marks bookings nobody checked in as NO_SHOW (DESIGN.md §10.4): at 23:55 IST for the day just
 * ending, and at startup for earlier days a stopped server missed. The startup run leaves today
 * alone, so a patient who is late can still be checked in.
 */
@Component
public class NoShowJob {

	private static final Logger log = LoggerFactory.getLogger(NoShowJob.class);

	private final ReceptionService reception;
	private final Clock clock;

	public NoShowJob(ReceptionService reception, Clock clock) {
		this.reception = reception;
		this.clock = clock;
	}

	@Scheduled(cron = "0 55 23 * * *", zone = "Asia/Kolkata")
	public void endOfDay() {
		run("end of day", LocalDate.now(clock));
	}

	@EventListener(ApplicationReadyEvent.class)
	public void atStartup() {
		run("startup", LocalDate.now(clock).minusDays(1));
	}

	private void run(String trigger, LocalDate lastDate) {
		try {
			int count = reception.markNoShows(lastDate);
			log.info("No-show job ({}): {} bookings up to {} marked NO_SHOW", trigger, count, lastDate);
		}
		catch (RuntimeException e) {
			log.error("No-show job ({}) failed", trigger, e);
		}
	}

}
