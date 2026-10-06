package com.vihan.hospital.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.vihan.hospital.config.AppProperties;

/**
 * Keeps the rolling booking window filled (FR-S2): every night at 00:30 IST, and once at startup
 * so a server that was down at night catches up. Safe to run any number of times.
 */
@Component
public class SlotGenerationJob {

	private static final Logger log = LoggerFactory.getLogger(SlotGenerationJob.class);

	private final SlotReconciler reconciler;
	private final boolean onStartup;

	public SlotGenerationJob(SlotReconciler reconciler, AppProperties props) {
		this.reconciler = reconciler;
		this.onStartup = props.scheduling().generateOnStartup();
	}

	@Scheduled(cron = "0 30 0 * * *", zone = "Asia/Kolkata")
	public void nightly() {
		run("nightly");
	}

	@EventListener(ApplicationReadyEvent.class)
	public void atStartup() {
		if (onStartup) {
			run("startup");
		}
	}

	private void run(String trigger) {
		long start = System.currentTimeMillis();
		try {
			SlotReconciler.Result r = reconciler.reconcileAllDoctors();
			log.info("Slot generation ({}): {} created, {} removed, {} blocked, {} reopened, {} bookings flagged in {} ms",
					trigger, r.created(), r.removed(), r.blocked(), r.reopened(), r.bookedSlotsNeedingAction().size(),
					System.currentTimeMillis() - start);
		}
		catch (RuntimeException e) {
			log.error("Slot generation ({}) failed", trigger, e);
		}
	}

}
