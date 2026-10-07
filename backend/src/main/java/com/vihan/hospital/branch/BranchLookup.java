package com.vihan.hospital.branch;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.vihan.hospital.common.error.ApiException.BadRequestException;

/**
 * Branch code ↔ id for staff filters ({@code ?branch=MLG}). Includes inactive branches, so older
 * reports still work after a branch is closed. Three rows: read fresh on each call.
 */
@Component
public class BranchLookup {

	private final BranchRepository branches;

	public BranchLookup(BranchRepository branches) {
		this.branches = branches;
	}

	/** All branches by id, in display order. */
	public Map<Long, Branch> byId() {
		Map<Long, Branch> map = new LinkedHashMap<>();
		branches.findAllByOrderByIdAsc().forEach(b -> map.put(b.getId(), b));
		return map;
	}

	/** null for a blank code ("all branches"); 400 for an unknown one. */
	public Long idOf(String code) {
		if (code == null || code.isBlank()) {
			return null;
		}
		return branches.findAllByOrderByIdAsc().stream()
			.filter(b -> b.getCode().equalsIgnoreCase(code.strip()))
			.map(Branch::getId)
			.findFirst()
			.orElseThrow(() -> new BadRequestException("UNKNOWN_BRANCH", "Unknown branch: " + code));
	}

}
