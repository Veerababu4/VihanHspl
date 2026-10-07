package com.vihan.hospital.dashboard;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vihan.hospital.appointment.Appointment.Status;
import com.vihan.hospital.appointment.AppointmentRepository;
import com.vihan.hospital.audit.AuditAction;
import com.vihan.hospital.audit.AuditService;
import com.vihan.hospital.branch.Branch;
import com.vihan.hospital.branch.BranchLookup;
import com.vihan.hospital.dashboard.DashboardDtos.BranchRow;
import com.vihan.hospital.dashboard.DashboardDtos.Counts;
import com.vihan.hospital.dashboard.DashboardDtos.DashboardSummary;
import com.vihan.hospital.dashboard.DashboardDtos.DoctorRow;
import com.vihan.hospital.doctor.Doctor;
import com.vihan.hospital.doctor.DoctorRepository;
import com.vihan.hospital.report.ReportTable;
import com.vihan.hospital.report.TableExport;
import com.vihan.hospital.schedule.SlotRepository;
import com.vihan.hospital.security.CurrentUser;
import com.vihan.hospital.user.Role;

/**
 * Network → branch → doctor counts for one day (FR-D1–D3, DESIGN.md §7, §10.5). Two small
 * queries (the day's appointment statuses and the open-slot counts), grouped in Java. Not cached:
 * at this hospital's size both queries take milliseconds, and reception wants live numbers.
 */
@Service
public class DashboardService {

	private record Key(Long branchId, Long doctorId) {
	}

	private final AppointmentRepository appointments;
	private final SlotRepository slots;
	private final DoctorRepository doctors;
	private final BranchLookup branches;
	private final AuditService audit;
	private final Clock clock;

	public DashboardService(AppointmentRepository appointments, SlotRepository slots, DoctorRepository doctors,
			BranchLookup branches, AuditService audit, Clock clock) {
		this.appointments = appointments;
		this.slots = slots;
		this.doctors = doctors;
		this.branches = branches;
		this.audit = audit;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public DashboardSummary summary(LocalDate date, String branchCode) {
		LocalDate day = date == null ? LocalDate.now(clock) : date;
		Long branchId = branches.idOf(branchCode);
		Long onlyDoctor = null;
		boolean doctorView = CurrentUser.role().orElse(null) == Role.DOCTOR;
		if (doctorView) {
			onlyDoctor = CurrentUser.doctorId().orElseThrow(() -> new AccessDeniedException("No doctor profile linked"));
		}
		Long doctorFilter = onlyDoctor;

		// network → branch → doctor → status counts (Collectors, as DESIGN.md §7 describes)
		Map<Long, Map<Long, Map<Status, Long>>> byBranch = appointments.statusesOn(day).stream()
			.filter(r -> branchId == null || branchId.equals(r[0]))
			.filter(r -> doctorFilter == null || doctorFilter.equals(r[1]))
			.collect(groupingBy(r -> (Long) r[0], groupingBy(r -> (Long) r[1],
					groupingBy(r -> (Status) r[2], () -> new EnumMap<>(Status.class), counting()))));

		Map<Key, Long> open = new HashMap<>();
		LocalDateTime now = LocalDateTime.now(clock);
		for (Object[] r : slots.countOpen(day, now)) {
			if ((branchId == null || branchId.equals(r[0])) && (doctorFilter == null || doctorFilter.equals(r[1]))) {
				open.put(new Key((Long) r[0], (Long) r[1]), (Long) r[2]);
			}
		}

		Map<Long, Branch> branchMap = branches.byId();
		Map<Long, Doctor> doctorMap = doctors.findAllByOrderByFullNameAsc().stream()
			.collect(Collectors.toMap(Doctor::getId, Function.identity()));

		// Every doctor/branch pair with an appointment or a free slot that day.
		Map<Key, Counts> perDoctor = new HashMap<>();
		byBranch.forEach((b, byDoctor) -> byDoctor.forEach((d, statuses) -> perDoctor.put(new Key(b, d),
				counts(statuses, open.getOrDefault(new Key(b, d), 0L)))));
		open.forEach((k, n) -> perDoctor.putIfAbsent(k, counts(Map.of(), n)));

		List<Long> branchOrder = new ArrayList<>(branchMap.keySet());
		List<DoctorRow> doctorRows = perDoctor.entrySet().stream()
			.map(e -> doctorRow(e.getKey(), e.getValue(), branchMap, doctorMap))
			.sorted(Comparator.comparing((DoctorRow r) -> branchOrder.indexOf(r.branchId()))
				.thenComparing(DoctorRow::doctorName, Comparator.nullsLast(Comparator.naturalOrder())))
			.toList();

		Map<Long, Counts> perBranch = new LinkedHashMap<>();
		branchMap.values().stream()
			.filter(b -> branchId == null ? b.isActive() : b.getId().equals(branchId))
			.forEach(b -> perBranch.put(b.getId(), Counts.ZERO));
		perDoctor.forEach((k, c) -> perBranch.merge(k.branchId(), c, Counts::plus));
		List<BranchRow> branchRows = perBranch.entrySet().stream()
			.map(e -> {
				Branch b = branchMap.get(e.getKey());
				return new BranchRow(b.getCode(), b.getName(), b.getCity(), e.getValue());
			})
			.toList();
		Counts network = branchRows.stream().map(BranchRow::counts).reduce(Counts.ZERO, Counts::plus);
		return new DashboardSummary(day, branchId == null ? null : branchMap.get(branchId).getCode(), doctorView, network,
				branchRows, doctorRows);
	}

	/** FR-D5: the doctor table plus branch and network totals. */
	@Transactional
	public ResponseEntity<byte[]> export(LocalDate date, String branchCode, String format) {
		TableExport.Format f = TableExport.parse(format);
		DashboardSummary s = summary(date, branchCode);
		List<List<Object>> rows = new ArrayList<>();
		s.doctors().forEach(d -> rows.add(row(d.branchCity(), d.doctorName(), d.speciality(), d.counts())));
		s.branches().forEach(b -> rows.add(row(b.branchCity(), "All doctors", "", b.counts())));
		rows.add(row("Network", "All doctors", "", s.network()));
		ReportTable table = new ReportTable("Appointments dashboard",
				s.date() + " · " + (s.branch() == null ? "All branches" : s.branch()),
				List.of("Branch", "Doctor", "Speciality", "Total", "Booked", "Checked in", "Completed", "Cancelled",
						"No-show", "Open slots"),
				rows);
		audit.record(AuditAction.REPORT_EXPORTED, "REPORT", "dashboard",
				"date=" + s.date() + " branch=" + (s.branch() == null ? "ALL" : s.branch()) + " format=" + f);
		return TableExport.download(table, f, "dashboard-" + s.date() + (s.branch() == null ? "" : "-" + s.branch()));
	}

	private static List<Object> row(String branch, String doctor, String speciality, Counts c) {
		return List.of(branch, doctor, speciality, c.total(), c.booked(), c.checkedIn(), c.completed(), c.cancelled(),
				c.noShow(), c.openSlots());
	}

	private static Counts counts(Map<Status, Long> s, long openSlots) {
		long booked = s.getOrDefault(Status.BOOKED, 0L);
		long checkedIn = s.getOrDefault(Status.CHECKED_IN, 0L);
		long completed = s.getOrDefault(Status.COMPLETED, 0L);
		long noShow = s.getOrDefault(Status.NO_SHOW, 0L);
		return new Counts(booked, checkedIn, completed, s.getOrDefault(Status.CANCELLED, 0L), noShow,
				booked + checkedIn + completed + noShow, openSlots);
	}

	private static DoctorRow doctorRow(Key k, Counts c, Map<Long, Branch> branchMap, Map<Long, Doctor> doctorMap) {
		Branch b = branchMap.get(k.branchId());
		Doctor d = doctorMap.get(k.doctorId());
		return new DoctorRow(k.doctorId(), d == null ? null : d.getFullName(), d == null ? null : d.getSpeciality().getName(),
				b.getId(), b.getCode(), b.getCity(), c);
	}

}
