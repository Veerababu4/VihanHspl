package com.vihan.hospital.schedule;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.vihan.hospital.security.CurrentUser;
import com.vihan.hospital.user.Role;

/**
 * Ownership rules for schedules and slots (DESIGN.md §4, §12.3): a doctor manages only their own,
 * IT Admin manages everyone's, receptionists may look (for counter booking).
 */
@Component
public class ScheduleAccess {

	public void requireView(Long doctorId) {
		Role role = CurrentUser.role().orElseThrow(() -> new AccessDeniedException("Not logged in"));
		if (role == Role.DOCTOR && !isOwn(doctorId)) {
			throw new AccessDeniedException("Doctors can only see their own schedule");
		}
	}

	public void requireEdit(Long doctorId) {
		Role role = CurrentUser.role().orElseThrow(() -> new AccessDeniedException("Not logged in"));
		boolean allowed = role == Role.IT_ADMIN || (role == Role.DOCTOR && isOwn(doctorId));
		if (!allowed) {
			throw new AccessDeniedException("Not allowed to change this doctor's schedule");
		}
	}

	private static boolean isOwn(Long doctorId) {
		return CurrentUser.doctorId().map(doctorId::equals).orElse(false);
	}

}
