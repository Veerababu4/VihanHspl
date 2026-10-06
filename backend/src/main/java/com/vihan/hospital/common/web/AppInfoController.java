package com.vihan.hospital.common.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vihan.hospital.config.AppProperties;

@RestController
@RequestMapping("/api/public/app-info")
public class AppInfoController {

	/** @param sampleData the UI shows a "SAMPLE DATA" banner when true */
	public record AppInfo(String name, boolean sampleData) {
	}

	private final AppProperties props;

	public AppInfoController(AppProperties props) {
		this.props = props;
	}

	@GetMapping
	public AppInfo get() {
		return new AppInfo("Vihan Multi Speciality Hospital", props.sampleData());
	}

}
