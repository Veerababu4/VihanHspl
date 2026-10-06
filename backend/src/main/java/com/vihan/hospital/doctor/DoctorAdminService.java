package com.vihan.hospital.doctor;

import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.vihan.hospital.audit.AuditAction;
import com.vihan.hospital.audit.AuditService;
import com.vihan.hospital.branch.Branch;
import com.vihan.hospital.branch.BranchRepository;
import com.vihan.hospital.common.error.ApiException.BadRequestException;
import com.vihan.hospital.common.error.ApiException.ConflictException;
import com.vihan.hospital.common.error.ApiException.NotFoundException;
import com.vihan.hospital.doctor.DoctorDtos.AdminDoctorDto;
import com.vihan.hospital.doctor.DoctorDtos.DoctorRequest;
import com.vihan.hospital.speciality.Speciality;
import com.vihan.hospital.speciality.SpecialityRepository;
import com.vihan.hospital.storage.FileUrls;
import com.vihan.hospital.storage.ImageService;
import com.vihan.hospital.storage.ImageService.StoredImage;

/**
 * IT Admin doctor management (FR-AD2). Deactivating hides the doctor from the public site;
 * blocking future slots and flagging bookings is added with scheduling (M4) and booking (M5).
 */
@Service
@Transactional
public class DoctorAdminService {

	private final DoctorRepository doctors;
	private final SpecialityRepository specialities;
	private final BranchRepository branches;
	private final ImageService images;
	private final FileUrls fileUrls;
	private final AuditService audit;

	public DoctorAdminService(DoctorRepository doctors, SpecialityRepository specialities, BranchRepository branches,
			ImageService images, FileUrls fileUrls, AuditService audit) {
		this.doctors = doctors;
		this.specialities = specialities;
		this.branches = branches;
		this.images = images;
		this.fileUrls = fileUrls;
		this.audit = audit;
	}

	@Transactional(readOnly = true)
	public List<AdminDoctorDto> list() {
		return doctors.findAllByOrderByFullNameAsc().stream().map(d -> AdminDoctorDto.from(d, fileUrls)).toList();
	}

	@Transactional(readOnly = true)
	public AdminDoctorDto get(Long id) {
		return AdminDoctorDto.from(find(id), fileUrls);
	}

	public AdminDoctorDto create(DoctorRequest req) {
		String regNo = blankToNull(req.registrationNo());
		if (regNo != null && doctors.existsByRegistrationNoIgnoreCase(regNo)) {
			throw duplicateRegNo();
		}
		Doctor d = new Doctor(req.fullName().strip(), speciality(req.specialityId()), req.qualification().strip(),
				req.practiceStartYear());
		apply(d, req);
		doctors.save(d);
		audit.record(AuditAction.DOCTOR_CREATED, "DOCTOR", d.getId(), d.getFullName());
		return AdminDoctorDto.from(d, fileUrls);
	}

	public AdminDoctorDto update(Long id, DoctorRequest req) {
		Doctor d = find(id);
		String regNo = blankToNull(req.registrationNo());
		if (regNo != null && doctors.existsByRegistrationNoIgnoreCaseAndIdNot(regNo, id)) {
			throw duplicateRegNo();
		}
		apply(d, req);
		audit.record(AuditAction.DOCTOR_UPDATED, "DOCTOR", id, "active=" + req.active());
		return AdminDoctorDto.from(d, fileUrls);
	}

	public AdminDoctorDto uploadPhoto(Long id, MultipartFile file) {
		Doctor d = find(id);
		StoredImage stored = images.store(file, ImageService.Kind.DOCTOR_PHOTO);
		images.cleanupOnCompletion(nullSafe(d.getPhotoUrl()), stored.keys());
		d.setPhotoUrl(stored.key());
		audit.record(AuditAction.DOCTOR_PHOTO_CHANGED, "DOCTOR", id, "uploaded");
		return AdminDoctorDto.from(d, fileUrls);
	}

	public AdminDoctorDto removePhoto(Long id) {
		Doctor d = find(id);
		images.cleanupOnCompletion(nullSafe(d.getPhotoUrl()), List.of());
		d.setPhotoUrl(null);
		audit.record(AuditAction.DOCTOR_PHOTO_CHANGED, "DOCTOR", id, "removed");
		return AdminDoctorDto.from(d, fileUrls);
	}

	private void apply(Doctor d, DoctorRequest req) {
		if (req.practiceStartYear() > Year.now().getValue()) {
			throw new BadRequestException("INVALID_PRACTICE_YEAR", "Practice start year cannot be in the future");
		}
		d.updateProfile(req.fullName().strip(), req.gender(), speciality(req.specialityId()),
				req.qualification().strip(), blankToNull(req.designation()), blankToNull(req.registrationNo()),
				req.practiceStartYear(), blankToNull(req.languages()), blankToNull(req.bio()), req.consultationFee(),
				req.active());
		d.replaceBranches(branchesFor(req.branchIds()));
	}

	private Set<Branch> branchesFor(Set<Long> ids) {
		List<Branch> found = branches.findAllById(ids);
		if (found.size() != ids.size()) {
			throw new BadRequestException("UNKNOWN_BRANCH", "One or more branches do not exist");
		}
		return new HashSet<>(found);
	}

	private Speciality speciality(Long id) {
		return specialities.findById(id)
			.orElseThrow(() -> new BadRequestException("UNKNOWN_SPECIALITY", "Speciality does not exist"));
	}

	private Doctor find(Long id) {
		return doctors.findWithDetailsById(id).orElseThrow(() -> new NotFoundException("Doctor", id));
	}

	private static ConflictException duplicateRegNo() {
		return new ConflictException("REGISTRATION_NO_EXISTS", "Another doctor already has this registration number");
	}

	private static List<String> nullSafe(String key) {
		return key == null ? List.of() : List.of(key);
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.strip();
	}

}
