package com.vihan.hospital.branch;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BranchService {

	private final BranchRepository branches;

	public BranchService(BranchRepository branches) {
		this.branches = branches;
	}

	public List<BranchDto> listActive() {
		return branches.findByActiveTrueOrderByIdAsc().stream().map(BranchDto::from).toList();
	}

}
