package com.vihan.hospital.branch;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.vihan.hospital.branch.BranchAdminDtos.AdminBranchDto;
import com.vihan.hospital.branch.BranchAdminDtos.BranchRequest;
import com.vihan.hospital.common.ReferenceDataCache;
import com.vihan.hospital.common.error.ApiException.BadRequestException;
import com.vihan.hospital.common.error.ApiException.ConflictException;
import com.vihan.hospital.common.error.ApiException.NotFoundException;
import com.vihan.hospital.doctor.DoctorDtos.DoctorSummaryDto;
import com.vihan.hospital.doctor.DoctorService;
import com.vihan.hospital.gallery.GalleryDtos.GalleryImageDto;
import com.vihan.hospital.gallery.GalleryService;
import com.vihan.hospital.storage.FileUrls;
import com.vihan.hospital.storage.ImageService;
import com.vihan.hospital.storage.ImageService.StoredImage;

@Service
@Transactional
public class BranchService {

	/** Branch page (FR-P4): details, doctors serving there, photos. */
	public record BranchDetailDto(BranchDto branch, List<DoctorSummaryDto> doctors, List<GalleryImageDto> gallery) {
	}

	private final BranchRepository branches;
	private final ReferenceDataCache cache;
	private final DoctorService doctorService;
	private final GalleryService galleryService;
	private final ImageService images;
	private final FileUrls fileUrls;

	public BranchService(BranchRepository branches, ReferenceDataCache cache, DoctorService doctorService,
			GalleryService galleryService, ImageService images, FileUrls fileUrls) {
		this.branches = branches;
		this.cache = cache;
		this.doctorService = doctorService;
		this.galleryService = galleryService;
		this.images = images;
		this.fileUrls = fileUrls;
	}

	// ---- public ----

	@Transactional(readOnly = true)
	public List<BranchDto> listActive() {
		return cache.branches();
	}

	@Transactional(readOnly = true)
	public BranchDetailDto detail(String code) {
		BranchDto branch = cache.branch(code).orElseThrow(() -> new NotFoundException("Branch", code));
		return new BranchDetailDto(branch, doctorService.list(branch.code(), null, null),
				galleryService.listPublic(branch.code()));
	}

	// ---- IT Admin (FR-AD3) ----

	@Transactional(readOnly = true)
	public List<AdminBranchDto> listAll() {
		return branches.findAllByOrderByIdAsc().stream().map(b -> AdminBranchDto.from(b, fileUrls)).toList();
	}

	public AdminBranchDto create(BranchRequest req) {
		if (branches.existsByCode(req.code())) {
			throw new ConflictException("BRANCH_CODE_EXISTS", "A branch with this code already exists");
		}
		Branch b = new Branch(req.code());
		apply(b, req);
		branches.save(b);
		cache.invalidate();
		return AdminBranchDto.from(b, fileUrls);
	}

	public AdminBranchDto update(Long id, BranchRequest req) {
		Branch b = find(id);
		if (!b.getCode().equals(req.code())) {
			throw new BadRequestException("BRANCH_CODE_FIXED", "Branch code cannot be changed (it is used in web links)");
		}
		apply(b, req);
		cache.invalidate();
		return AdminBranchDto.from(b, fileUrls);
	}

	public AdminBranchDto uploadImage(Long id, MultipartFile file) {
		Branch b = find(id);
		StoredImage stored = images.store(file, ImageService.Kind.BRANCH_IMAGE);
		images.cleanupOnCompletion(b.getImageUrl() == null ? List.of() : List.of(b.getImageUrl()), stored.keys());
		b.setImageUrl(stored.key());
		cache.invalidate();
		return AdminBranchDto.from(b, fileUrls);
	}

	private void apply(Branch b, BranchRequest req) {
		b.updateDetails(req.name().strip(), req.address().strip(), req.city().strip(), blankToNull(req.district()),
				blankToNull(req.pincode()), blankToNull(req.phone()), blankToNull(req.emergencyPhone()),
				blankToNull(req.email()), blankToNull(req.mapUrl()), blankToNull(req.openingHours()), req.active());
	}

	private Branch find(Long id) {
		return branches.findById(id).orElseThrow(() -> new NotFoundException("Branch", id));
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.strip();
	}

}
