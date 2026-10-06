package com.vihan.hospital.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Sends a notification only once the booking change has been committed, so a patient is never
 * told about a booking that was rolled back. A failing sender never breaks the booking.
 */
@Component
public class Notifications {

	private static final Logger log = LoggerFactory.getLogger(Notifications.class);

	private final NotificationService sender;

	public Notifications(NotificationService sender) {
		this.sender = sender;
	}

	public void afterCommit(NotificationService.Message message) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			sendSafely(message);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				sendSafely(message);
			}
		});
	}

	private void sendSafely(NotificationService.Message message) {
		try {
			sender.send(message);
		}
		catch (RuntimeException e) {
			log.error("Notification {} for {} failed", message.event(), message.bookingRef(), e);
		}
	}

}
