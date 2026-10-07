package com.vihan.hospital.dashboard;

import java.time.LocalDate;
import java.util.List;

/** Dashboard counts (DESIGN.md §5.6, §10.5). */
public final class DashboardDtos {

	private DashboardDtos() {
	}

	/**
	 * @param total     every appointment except cancelled ones
	 * @param openSlots slots still free (for today: only those not yet started)
	 */
	public record Counts(long booked, long checkedIn, long completed, long cancelled, long noShow, long total,
			long openSlots) {

		public static final Counts ZERO = new Counts(0, 0, 0, 0, 0, 0, 0);

		public Counts plus(Counts o) {
			return new Counts(booked + o.booked, checkedIn + o.checkedIn, completed + o.completed,
					cancelled + o.cancelled, noShow + o.noShow, total + o.total, openSlots + o.openSlots);
		}
	}

	public record DoctorRow(Long doctorId, String doctorName, String speciality, Long branchId, String branchCode,
			String branchCity, Counts counts) {
	}

	public record BranchRow(String branchCode, String branchName, String branchCity, Counts counts) {
	}

	/**
	 * @param branch the branch filter, null = all branches
	 * @param doctorOnly true when a doctor is looking: every figure is their own
	 */
	public record DashboardSummary(LocalDate date, String branch, boolean doctorOnly, Counts network,
			List<BranchRow> branches, List<DoctorRow> doctors) {
	}

}
