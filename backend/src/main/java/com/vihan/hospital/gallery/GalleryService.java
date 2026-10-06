package com.vihan.hospital.gallery;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.vihan.hospital.branch.BranchDto;
import com.vihan.hospital.branch.BranchRepository;
import com.vihan.hospital.common.ReferenceDataCache;
import com.vihan.hospital.common.error.ApiException.BadRequestException;
import com.vihan.hospital.common.error.ApiException.NotFoundException;
import com.vihan.hospital.gallery.GalleryDtos.AdminGalleryImageDto;
import com.vihan.hospital.gallery.GalleryDtos.GalleryImageDto;
import com.vihan.hospital.gallery.GalleryDtos.GalleryImageRequest;
import com.vihan.hospital.storage.FileUrls;
import com.vihan.hospital.storage.ImageService;
import com.vihan.hospital.storage.ImageService.StoredImage;

/**
 * Public gallery (FR-P5) and IT Admin management (FR-AD4). Unlike master data, gallery images are
 * deleted for real (row and files): nothing else references them.
 */
@Service
@Transactional
public class GalleryService {

	private final GalleryImageRepository gallery;
	private final BranchRepository branches;
	private final ReferenceDataCache cache;
	private final ImageService images;
	private final FileUrls fileUrls;

	public GalleryService(GalleryImageRepository gallery, BranchRepository branches, ReferenceDataCache cache,
			ImageService images, FileUrls fileUrls) {
		this.gallery = gallery;
		this.branches = branches;
		this.cache = cache;
		this.images = images;
		this.fileUrls = fileUrls;
	}

	/** @param branchCode null = all images; otherwise only that branch's images */
	@Transactional(readOnly = true)
	public List<GalleryImageDto> listPublic(String branchCode) {
		Map<Long, String> codeById = cache.branches().stream()
			.collect(Collectors.toMap(BranchDto::id, BranchDto::code));
		List<GalleryImage> rows;
		if (branchCode == null || branchCode.isBlank()) {
			rows = gallery.findByActiveTrueOrderBySortOrderAscIdAsc();
		}
		else {
			BranchDto branch = cache.branch(branchCode).orElseThrow(() -> new NotFoundException("Branch", branchCode));
			rows = gallery.findByActiveTrueAndBranchIdOrderBySortOrderAscIdAsc(branch.id());
		}
		return rows.stream()
			.filter(g -> g.getBranchId() == null || codeById.containsKey(g.getBranchId()))   // hide closed branches
			.map(g -> new GalleryImageDto(g.getId(), codeById.get(g.getBranchId()), g.getTitle(), g.getCaption(),
					fileUrls.toUrl(g.getImageUrl()), fileUrls.toUrl(g.getThumbUrl())))
			.toList();
	}

	@Transactional(readOnly = true)
	public List<AdminGalleryImageDto> listAdmin() {
		return gallery.findAllByOrderBySortOrderAscIdAsc().stream()
			.map(g -> AdminGalleryImageDto.from(g, fileUrls))
			.toList();
	}

	public AdminGalleryImageDto upload(MultipartFile file, GalleryImageRequest req) {
		checkBranch(req.branchId());
		StoredImage stored = images.store(file, ImageService.Kind.GALLERY);
		images.cleanupOnCompletion(List.of(), stored.keys());
		GalleryImage g = new GalleryImage(stored.key(), stored.thumbKey());
		apply(g, req);
		gallery.save(g);
		return AdminGalleryImageDto.from(g, fileUrls);
	}

	public AdminGalleryImageDto update(Long id, GalleryImageRequest req) {
		GalleryImage g = find(id);
		checkBranch(req.branchId());
		apply(g, req);
		return AdminGalleryImageDto.from(g, fileUrls);
	}

	public void delete(Long id) {
		GalleryImage g = find(id);
		images.cleanupOnCompletion(List.of(g.getImageUrl(), g.getThumbUrl()), List.of());
		gallery.delete(g);
	}

	private void apply(GalleryImage g, GalleryImageRequest req) {
		g.updateDetails(req.branchId(), blankToNull(req.title()), blankToNull(req.caption()),
				req.sortOrder() == null ? 0 : req.sortOrder(), req.active());
	}

	private void checkBranch(Long branchId) {
		if (branchId != null && !branches.existsById(branchId)) {
			throw new BadRequestException("UNKNOWN_BRANCH", "Branch does not exist");
		}
	}

	private GalleryImage find(Long id) {
		return gallery.findById(id).orElseThrow(() -> new NotFoundException("Gallery image", id));
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.strip();
	}

}
