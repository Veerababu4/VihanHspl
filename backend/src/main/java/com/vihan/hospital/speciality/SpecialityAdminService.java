package com.vihan.hospital.speciality;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vihan.hospital.common.ReferenceDataCache;
import com.vihan.hospital.common.error.ApiException.ConflictException;
import com.vihan.hospital.common.error.ApiException.NotFoundException;
import com.vihan.hospital.doctor.DoctorRepository;
import com.vihan.hospital.speciality.SpecialityDtos.AdminSpecialityDto;
import com.vihan.hospital.speciality.SpecialityDtos.SpecialityRequest;

@Service
@Transactional
public class SpecialityAdminService {

	private final SpecialityRepository specialities;
	private final DoctorRepository doctors;
	private final ReferenceDataCache cache;

	public SpecialityAdminService(SpecialityRepository specialities, DoctorRepository doctors, ReferenceDataCache cache) {
		this.specialities = specialities;
		this.doctors = doctors;
		this.cache = cache;
	}

	@Transactional(readOnly = true)
	public List<AdminSpecialityDto> list() {
		return specialities.findAllByOrderByNameAsc().stream().map(AdminSpecialityDto::from).toList();
	}

	public AdminSpecialityDto create(SpecialityRequest req) {
		if (specialities.existsByNameIgnoreCase(req.name().strip())) {
			throw new ConflictException("SPECIALITY_EXISTS", "A speciality with this name already exists");
		}
		Speciality s = new Speciality(req.name(), req.description(), req.active());
		specialities.save(s);
		cache.invalidate();
		return AdminSpecialityDto.from(s);
	}

	public AdminSpecialityDto update(Long id, SpecialityRequest req) {
		Speciality s = specialities.findById(id).orElseThrow(() -> new NotFoundException("Speciality", id));
		if (specialities.existsByNameIgnoreCaseAndIdNot(req.name().strip(), id)) {
			throw new ConflictException("SPECIALITY_EXISTS", "A speciality with this name already exists");
		}
		if (s.isActive() && !req.active() && doctors.existsBySpecialityIdAndActiveTrue(id)) {
			throw new ConflictException("SPECIALITY_IN_USE",
					"Active doctors belong to this speciality. Move or deactivate them first.");
		}
		s.update(req.name(), req.description(), req.active());
		cache.invalidate();
		return AdminSpecialityDto.from(s);
	}

}
