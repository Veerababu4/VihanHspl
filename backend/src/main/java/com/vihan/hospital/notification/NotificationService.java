package com.vihan.hospital.notification;

import java.time.LocalDateTime;

/**
 * SMS / WhatsApp hook (DESIGN.md §19.8). Phase 1 only logs; Phase 2 adds an implementation for
 * MSG91 / Twilio / WhatsApp Business (with DLT templates) without touching booking code.
 */
public interface NotificationService {

	enum Event { BOOKED, CANCELLED, RESCHEDULED, NEEDS_ACTION, REMINDER }

	/** What a message needs; a snapshot, so it can be sent after the transaction has finished. */
	record Message(Event event, String mobile, String patientName, String bookingRef, LocalDateTime startAt,
			Integer tokenNo, String doctorName, String branchCity) {
	}

	void send(Message message);

}
