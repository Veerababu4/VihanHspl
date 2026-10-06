package com.vihan.hospital.common;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.vihan.hospital.branch.BranchDto;
import com.vihan.hospital.branch.BranchRepository;
import com.vihan.hospital.speciality.SpecialityDtos.SpecialityDto;
import com.vihan.hospital.speciality.SpecialityRepository;
import com.vihan.hospital.storage.FileUrls;

/**
 * In-memory copy of the active branches and specialities, which nearly every public page reads
 * (DESIGN.md §7). Readers get an immutable snapshot; admin changes call {@link #invalidate()}
 * and the next read reloads. Only a few dozen rows, so a full reload is cheap.
 */
@Component
public class ReferenceDataCache {

	/** Branches keyed by code, in display order (MLG, HZN, KDD); specialities sorted by name. */
	private record Snapshot(Map<String, BranchDto> branchesByCode, List<SpecialityDto> specialities) {
	}

	private final BranchRepository branchRepository;
	private final SpecialityRepository specialityRepository;
	private final FileUrls fileUrls;
	private volatile Snapshot snapshot;

	public ReferenceDataCache(BranchRepository branchRepository, SpecialityRepository specialityRepository,
			FileUrls fileUrls) {
		this.branchRepository = branchRepository;
		this.specialityRepository = specialityRepository;
		this.fileUrls = fileUrls;
	}

	public List<BranchDto> branches() {
		return List.copyOf(current().branchesByCode().values());
	}

	public Optional<BranchDto> branch(String code) {
		return Optional.ofNullable(current().branchesByCode().get(code.toUpperCase()));
	}

	public List<SpecialityDto> specialities() {
		return current().specialities();
	}

	/**
	 * Drops the snapshot. Inside a transaction this waits until the transaction ends, so a concurrent
	 * reader can't reload and keep the old data; on rollback it is dropped too, in case a read inside
	 * the transaction loaded uncommitted rows.
	 */
	public void invalidate() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					snapshot = null;
				}
			});
		}
		else {
			snapshot = null;
		}
	}

	private Snapshot current() {
		Snapshot s = snapshot;
		if (s == null) {
			s = load();
			snapshot = s;
		}
		return s;
	}

	private Snapshot load() {
		Map<String, BranchDto> branches = new LinkedHashMap<>();
		branchRepository.findByActiveTrueOrderByIdAsc()
			.forEach(b -> branches.put(b.getCode(), BranchDto.from(b, fileUrls)));
		List<SpecialityDto> specialities = specialityRepository.findByActiveTrueOrderByNameAsc().stream()
			.map(SpecialityDto::from)
			.toList();
		return new Snapshot(Collections.unmodifiableMap(branches), specialities);
	}

}
