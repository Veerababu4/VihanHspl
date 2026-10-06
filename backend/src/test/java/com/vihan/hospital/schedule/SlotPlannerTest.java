package com.vihan.hospital.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.vihan.hospital.schedule.SlotPlanner.Closures;
import com.vihan.hospital.schedule.SlotPlanner.PlannedSlot;

class SlotPlannerTest {

	private static final Long MLG = 1L;
	private static final Long KDD = 3L;
	private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);
	private static final Closures NONE = new Closures(List.of(), List.of());

	private static DoctorSchedule template(long id, Long branch, int dow, String start, String end, int minutes) {
		DoctorSchedule s = new DoctorSchedule(9L);
		s.update(branch, dow, start, end, minutes, LocalDate.of(2026, 1, 1), null);
		ReflectionTestUtils.setField(s, "id", id);
		return s;
	}

	@Test
	void sessionIsSplitIntoSlotsWithTokens() {
		var plan = SlotPlanner.plan(List.of(template(1, MLG, 1, "10:00", "11:00", 20)), Set.of(MLG), NONE, MONDAY, MONDAY);
		assertThat(plan.values()).extracting(PlannedSlot::token).containsExactly(1, 2, 3);
		assertThat(plan.firstKey()).isEqualTo(MONDAY.atTime(10, 0));
		assertThat(plan.lastEntry().getValue().end()).isEqualTo(MONDAY.atTime(11, 0));
	}

	@Test
	void partialLastSlotIsDropped() {
		var plan = SlotPlanner.plan(List.of(template(1, MLG, 1, "10:00", "10:50", 20)), Set.of(MLG), NONE, MONDAY, MONDAY);
		assertThat(plan).hasSize(2);   // 10:00, 10:20; 10:40-11:00 would run past the end
	}

	@Test
	void tokensContinueAcrossSessionsOfTheSameDayAndBranch() {
		var plan = SlotPlanner.plan(List.of(
				template(1, MLG, 1, "17:00", "17:40", 20),
				template(2, MLG, 1, "10:00", "10:40", 20)), Set.of(MLG), NONE, MONDAY, MONDAY);
		assertThat(plan.values()).extracting(PlannedSlot::token).containsExactly(1, 2, 3, 4);
		assertThat(plan.get(MONDAY.atTime(17, 0)).token()).isEqualTo(3);
	}

	@Test
	void onlyMatchingWeekdaysAndEffectiveDates() {
		DoctorSchedule limited = template(1, MLG, 1, "10:00", "10:20", 20);
		limited.update(MLG, 1, "10:00", "10:20", 20, MONDAY.plusDays(7), MONDAY.plusDays(7));
		var plan = SlotPlanner.plan(List.of(limited, template(2, KDD, 2, "10:00", "10:20", 20)), Set.of(MLG, KDD), NONE,
				MONDAY, MONDAY.plusDays(13));
		assertThat(plan.keySet()).containsExactly(
				MONDAY.plusDays(1).atTime(10, 0),     // Tuesday at KDD
				MONDAY.plusDays(7).atTime(10, 0),     // the only Monday in effect
				MONDAY.plusDays(8).atTime(10, 0));    // next Tuesday
	}

	@Test
	void leaveRemovesEveryBranchAndHolidayOnlyItsBranch() {
		var templates = List.of(template(1, MLG, 1, "10:00", "10:20", 20), template(2, KDD, 2, "10:00", "10:20", 20));
		LocalDate tuesday = MONDAY.plusDays(1);

		var onLeave = new Closures(List.of(new DoctorLeave(9L, MONDAY, tuesday, null)), List.of());
		assertThat(SlotPlanner.plan(templates, Set.of(MLG, KDD), onLeave, MONDAY, tuesday)).isEmpty();

		var mlgHoliday = new Closures(List.of(), List.of(new BranchHoliday(MLG, MONDAY, "Festival")));
		assertThat(SlotPlanner.plan(templates, Set.of(MLG, KDD), mlgHoliday, MONDAY, tuesday).keySet())
			.containsExactly(tuesday.atTime(10, 0));

		var allBranches = new Closures(List.of(), List.of(new BranchHoliday(null, tuesday, "National holiday")));
		assertThat(SlotPlanner.plan(templates, Set.of(MLG, KDD), allBranches, MONDAY, tuesday).keySet())
			.containsExactly(MONDAY.atTime(10, 0));
		assertThat(allBranches.reason(tuesday, KDD)).isEqualTo(Slot.BlockReason.HOLIDAY);
		assertThat(onLeave.reason(MONDAY, MLG)).isEqualTo(Slot.BlockReason.LEAVE);
	}

	@Test
	void branchesTheDoctorNoLongerServesAreIgnored() {
		var plan = SlotPlanner.plan(List.of(template(1, MLG, 1, "10:00", "10:20", 20)), Set.of(KDD), NONE, MONDAY, MONDAY);
		assertThat(plan).isEmpty();
	}

	@Test
	void sessionsEndingNearMidnightDontWrapToTheNextDay() {
		NavigableMap<LocalDateTime, PlannedSlot> plan = SlotPlanner.plan(
				List.of(template(1, MLG, 1, "23:00", "23:59", 30)), Set.of(MLG), NONE, MONDAY, MONDAY);
		assertThat(plan.keySet()).containsExactly(MONDAY.atTime(23, 0));
	}

	@Test
	void overlapNeedsSameWeekdayOverlappingTimesAndDates() {
		DoctorSchedule a = template(1, MLG, 1, "10:00", "14:00", 20);
		assertThat(SlotPlanner.overlaps(a, template(2, KDD, 1, "13:00", "15:00", 20))).isTrue();
		assertThat(SlotPlanner.overlaps(a, template(3, KDD, 1, "14:00", "16:00", 20))).isFalse();   // back to back
		assertThat(SlotPlanner.overlaps(a, template(4, KDD, 2, "10:00", "14:00", 20))).isFalse();   // other day

		DoctorSchedule later = template(5, KDD, 1, "10:00", "14:00", 20);
		a.update(MLG, 1, "10:00", "14:00", 20, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
		later.update(KDD, 1, "10:00", "14:00", 20, LocalDate.of(2026, 7, 1), null);
		assertThat(SlotPlanner.overlaps(a, later)).isFalse();   // same times, different date ranges

		assertThat(SlotPlanner.findClashes(List.of(template(6, MLG, 3, "10:00", "12:00", 20),
				template(7, KDD, 3, "11:00", "13:00", 20), template(8, KDD, 3, "15:00", "16:00", 20)))).hasSize(1);
	}

}
