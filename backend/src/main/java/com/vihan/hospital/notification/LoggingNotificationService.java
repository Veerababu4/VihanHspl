package com.vihan.hospital.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.vihan.hospital.common.util.Masking;

/** Phase 1: writes what would be sent to the log (mobile masked, no patient name). */
@Service
public class LoggingNotificationService implements NotificationService {

	private static final Logger log = LoggerFactory.getLogger("notifications");

	@Override
	public void send(Message m) {
		log.info("[would send SMS] {} to {}: {} {} token {} with {} at {}", m.event(), Masking.mobile(m.mobile()),
				m.bookingRef(), m.startAt(), m.tokenNo(), m.doctorName(), m.branchCity());
	}

}
