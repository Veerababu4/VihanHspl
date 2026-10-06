package com.vihan.hospital.doctor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vihan.hospital.branch.Branch;
import com.vihan.hospital.common.error.ApiException.NotFoundException;
import com.vihan.hospital.doctor.DoctorDtos.BranchAvailability;
import com.vihan.hospital.doctor.DoctorDtos.DayAvailability;
import com.vihan.hospital.doctor.DoctorDtos.DoctorDetailDto;
import com.vihan.hospital.doctor.DoctorDtos.DoctorSummaryDto;
import com.vihan.hospital.doctor.DoctorDtos.Session;
import com.vihan.hospital.schedule.DoctorSchedule;
import com.vihan.hospital.schedule.DoctorScheduleRepository;
import com.vihan.hospital.storage.FileUrls;

/** Public doctor directory and profiles. */
@Service
@Transactional(readOnly = true)
public class DoctorService {

	private static final Comparator<Doctor> BY_NAME =
			Comparator.comparing(d -> sortName(d.getFullName()));

	private final DoctorRepository doctors;
	private final DoctorScheduleRepository schedules;
	private final FileUrls fileUrls;

	public DoctorService(DoctorRepository doctors, DoctorScheduleRepository schedules, FileUrls fileUrls) {
		this.doctors = doctors;
		this.schedules = schedules;
		this.fileUrls = fileUrls;
	}

	/**
	 * Directory with optional filters (FR-P2). All active doctors are loaded in one query and
	 * filtered in memory: the network has at most a few hundred doctors.
	 *
	 * @param branchCode   e.g. "KDD"; null = all branches
	 * @param specialityId null = all specialities
	 * @param query        case-insensitive match on name, speciality or qualification
	 */
	public List<DoctorSummaryDto> list(String branchCode, Long specialityId, String query) {
		Predicate<Doctor> filter = d -> true;
		if (branchCode != null && !branchCode.isBlank()) {
			String code = branchCode.strip().toUpperCase(Locale.ROOT);
			filter = filter.and(d -> d.getBranches().stream().anyMatch(b -> b.isActive() && b.getCode().equals(code)));
		}
		if (specialityId != null) {
			filter = filter.and(d -> d.getSpeciality().getId().equals(specialityId));
		}
		if (query != null && !query.isBlank()) {
			String q = query.strip().toLowerCase(Locale.ROOT);
			filter = filter.and(d -> d.getFullName().toLowerCase(Locale.ROOT).contains(q)
					|| d.getSpeciality().getName().toLowerCase(Locale.ROOT).contains(q)
					|| d.getQualification().toLowerCase(Locale.ROOT).contains(q));
		}
		return doctors.findByActiveTrue().stream()
			.filter(d -> d.getSpeciality().isActive())
			.filter(filter)
			.sorted(BY_NAME)
			.map(d -> DoctorSummaryDto.from(d, fileUrls))
			.toList();
	}

	public DoctorDetailDto get(Long id) {
		Doctor d = doctors.findWithDetailsById(id)
			.filter(Doctor::isActive)
			.orElseThrow(() -> new NotFoundException("Doctor", id));
		return new DoctorDetailDto(d.getId(), d.getFullName(), d.getGender(), DoctorDtos.specialityRef(d),
				d.getQualification(), d.getDesignation(), d.getRegistrationNo(), d.experienceYears(),
				d.getLanguages(), d.getBio(), fileUrls.toUrl(d.getPhotoUrl()), d.getConsultationFee(),
				DoctorDtos.branchRefs(d), weeklyAvailability(d));
	}

	/**
	 * Groups the doctor's current weekly templates as branch → weekday → sessions. TreeMaps keep
	 * branches in display order and weekdays Monday-first.
	 */
	private List<BranchAvailability> weeklyAvailability(Doctor d) {
		Map<Long, Branch> activeBranches = new TreeMap<>();
		d.getBranches().stream().filter(Branch::isActive).forEach(b -> activeBranches.put(b.getId(), b));

		Map<Long, TreeMap<Integer, List<Session>>> byBranch = new TreeMap<>();
		for (DoctorSchedule s : schedules.findInEffect(d.getId(), LocalDate.now())) {
			if (!activeBranches.containsKey(s.getBranchId())) {
				continue;   // doctor no longer serves that branch, or the branch is closed
			}
			byBranch.computeIfAbsent(s.getBranchId(), k -> new TreeMap<>())
				.computeIfAbsent(s.getDayOfWeek(), k -> new ArrayList<>())
				.add(new Session(s.getStartTime(), s.getEndTime(), s.getSlotMinutes()));
		}

		List<BranchAvailability> result = new ArrayList<>();
		byBranch.forEach((branchId, days) -> {
			Branch b = activeBranches.get(branchId);
			List<DayAvailability> dayList = days.entrySet().stream()
				.map(e -> new DayAvailability(e.getKey(), e.getValue()))
				.toList();
			result.add(new BranchAvailability(b.getCode(), b.getCity(), dayList));
		});
		return result;
	}

	/** "Dr. Ravi Teja" sorts under R, not D. */
	static String sortName(String fullName) {
		return fullName.replaceFirst("(?i)^dr\\.?\\s+", "").toLowerCase(Locale.ROOT);
	}

}
