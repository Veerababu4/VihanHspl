package com.vihan.hospital.schedule;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.vihan.hospital.common.util.Masking;
import com.vihan.hospital.schedule.ScheduleDtos.AffectedBooking;
import com.vihan.hospital.schedule.ScheduleDtos.ChangeSummary;

/** Turns a reconcile result into what the screen shows: counts plus bookings to call (mobile masked). */
@Component
public class ChangeSummaries {

	private final SlotReconciler reconciler;

	public ChangeSummaries(SlotReconciler reconciler) {
		this.reconciler = reconciler;
	}

	public ChangeSummary of(SlotReconciler.Result r) {
		List<AffectedBooking> bookings = reconciler.appointmentsOnSlots(r.bookedSlotsNeedingAction()).stream()
			.map(ChangeSummaries::toBooking)
			.toList();
		return new ChangeSummary(r.created(), r.removed(), r.blocked(), r.reopened(), bookings);
	}

	private static AffectedBooking toBooking(Map<String, Object> row) {
		Object token = row.get("TOKEN_NO");
		return new AffectedBooking((String) row.get("BOOKING_REF"), (String) row.get("PATIENT_NAME"),
				Masking.mobile((String) row.get("PATIENT_MOBILE")),
				row.get("START_AT") instanceof Timestamp ts ? ts.toLocalDateTime() : null,
				token == null ? null : ((Number) token).intValue(), (String) row.get("CITY"));
	}

}
