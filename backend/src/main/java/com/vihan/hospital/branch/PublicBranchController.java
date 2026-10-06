package com.vihan.hospital.branch;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/branches")
public class PublicBranchController {

	private final BranchService branchService;

	public PublicBranchController(BranchService branchService) {
		this.branchService = branchService;
	}

	@GetMapping
	public List<BranchDto> list() {
		return branchService.listActive();
	}

}
