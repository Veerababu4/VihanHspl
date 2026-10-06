package com.vihan.hospital.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/**
 * Pure planning logic (DESIGN.md §10.2): which slots a doctor <em>should</em> have between two dates,
 * given the weekly templates, the branches the doctor serves, leave and holidays. No database
 * access, so it is easy to test; {@link SlotReconciler} compares the plan with the database.
 */
public final class SlotPlanner {

	/** A slot the timetable asks for. {@code token} = position in the doctor's day at that branch. */
	public record PlannedSlot(Long branchId, LocalDateTime start, LocalDateTime end, int token) {
	}

	/** Leave and holidays, i.e. the days (per branch) on which no slots may exist. */
	public record Closures(Collection<DoctorLeave> leaves, Collection<BranchHoliday> holidays) {

		/** LEAVE or HOLIDAY if the doctor can't work at that branch on that date, else null. */
		public Slot.BlockReason reason(LocalDate date, Long branchId) {
			if (leaves.stream().anyMatch(l -> l.covers(date))) {
				return Slot.BlockReason.LEAVE;
			}
			if (holidays.stream().anyMatch(h -> h.getHolidayDate().equals(date) && h.appliesTo(branchId))) {
				return Slot.BlockReason.HOLIDAY;
			}
			return null;
		}
	}

	private SlotPlanner() {
	}

	/**
	 * @param templates      the doctor's active weekly templates
	 * @param servedBranches active branches the doctor currently serves; templates elsewhere are ignored
	 * @return planned slots keyed by start time (a doctor can be in only one place at a time)
	 */
	public static NavigableMap<LocalDateTime, PlannedSlot> plan(Collection<DoctorSchedule> templates,
			Set<Long> servedBranches, Closures closures, LocalDate from, LocalDate to) {
		NavigableMap<LocalDateTime, PlannedSlot> result = new TreeMap<>();
		for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
			planDay(date, templates, servedBranches, closures, result);
		}
		return result;
	}

	private static void planDay(LocalDate date, Collection<DoctorSchedule> templates, Set<Long> servedBranches,
			Closures closures, NavigableMap<LocalDateTime, PlannedSlot> result) {
		int weekday = date.getDayOfWeek().getValue();
		// Branch → that day's slot times, in time order (tokens are numbered per branch per day).
		Map<Long, NavigableMap<LocalDateTime, LocalDateTime>> byBranch = new TreeMap<>();
		for (DoctorSchedule t : templates) {
			if (t.getDayOfWeek() != weekday || !t.inEffectOn(date) || !servedBranches.contains(t.getBranchId())
					|| closures.reason(date, t.getBranchId()) != null) {
				continue;
			}
			LocalTime end = t.end();
			for (LocalTime time = t.start(); !time.plusMinutes(t.getSlotMinutes()).isAfter(end)
					&& time.plusMinutes(t.getSlotMinutes()).isAfter(time); time = time.plusMinutes(t.getSlotMinutes())) {
				LocalDateTime start = date.atTime(time);
				byBranch.computeIfAbsent(t.getBranchId(), k -> new TreeMap<>())
					.putIfAbsent(start, start.plusMinutes(t.getSlotMinutes()));
			}
		}
		byBranch.forEach((branchId, times) -> {
			int token = 1;
			for (var e : times.entrySet()) {
				// putIfAbsent: if two branches ever claimed the same minute (blocked by validation),
				// the first one wins rather than double-booking the doctor.
				if (result.putIfAbsent(e.getKey(), new PlannedSlot(branchId, e.getKey(), e.getValue(), token)) == null) {
					token++;
				}
			}
		});
	}

	/** True if two sessions on the same weekday overlap in time and in effective dates. */
	public static boolean overlaps(DoctorSchedule a, DoctorSchedule b) {
		if (a.getDayOfWeek() != b.getDayOfWeek()) {
			return false;
		}
		boolean datesOverlap = (b.getEffectiveTo() == null || !a.getEffectiveFrom().isAfter(b.getEffectiveTo()))
				&& (a.getEffectiveTo() == null || !b.getEffectiveFrom().isAfter(a.getEffectiveTo()));
		return datesOverlap && a.start().isBefore(b.end()) && b.start().isBefore(a.end());
	}

	/**
	 * Sessions of a doctor that clash with each other (FR-S6), found by sorting each weekday's
	 * sessions by start time and comparing neighbours — plus a full check when effective dates differ.
	 */
	public static List<DoctorSchedule[]> findClashes(Collection<DoctorSchedule> templates) {
		List<DoctorSchedule> sorted = new ArrayList<>(templates);
		sorted.sort(Comparator.comparingInt(DoctorSchedule::getDayOfWeek).thenComparing(DoctorSchedule::start));
		List<DoctorSchedule[]> clashes = new ArrayList<>();
		for (int i = 0; i < sorted.size(); i++) {
			for (int j = i + 1; j < sorted.size() && sorted.get(j).getDayOfWeek() == sorted.get(i).getDayOfWeek()
					&& sorted.get(j).start().isBefore(sorted.get(i).end().plusMinutes(1)); j++) {
				if (overlaps(sorted.get(i), sorted.get(j))) {
					clashes.add(new DoctorSchedule[] { sorted.get(i), sorted.get(j) });
				}
			}
		}
		return clashes;
	}

}
