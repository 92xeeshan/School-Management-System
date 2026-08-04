package com.schoolms.school;

import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.security.SecurityUtils;
import com.schoolms.school.dto.SchoolDto;
import com.schoolms.school.dto.SchoolRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository schoolRepository;

    @Transactional(readOnly = true)
    public List<SchoolDto> list() {
        return schoolRepository.findAll().stream().map(SchoolDto::from).toList();
    }

    @Transactional(readOnly = true)
    public SchoolDto get(UUID id) {
        return SchoolDto.from(findSchool(id));
    }

    @Transactional(readOnly = true)
    public SchoolDto current() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        return SchoolDto.from(findSchool(schoolId));
    }

    @Transactional
    public SchoolDto create(SchoolRequest request) {
        if (schoolRepository.existsByCode(request.code())) {
            throw new BusinessException("school.code_exists");
        }
        School school = new School();
        apply(school, request);
        return SchoolDto.from(schoolRepository.save(school));
    }

    @Transactional
    public SchoolDto update(UUID id, SchoolRequest request) {
        School school = findSchool(id);
        apply(school, request);
        return SchoolDto.from(schoolRepository.save(school));
    }

    private School findSchool(UUID id) {
        return schoolRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("school", id));
    }

    private void apply(School school, SchoolRequest request) {
        school.setCode(request.code());
        school.setName(request.name());
        school.setAddress(request.address());
        school.setPhone(request.phone());
        school.setEmail(request.email());
        if (request.currency() != null) {
            school.setCurrency(request.currency());
        }
        if (request.defaultLocale() != null) {
            school.setDefaultLocale(request.defaultLocale());
        }
        if (request.timezone() != null) {
            school.setTimezone(request.timezone());
        }
        if (request.academicStartMonth() != null) {
            school.setAcademicStartMonth(request.academicStartMonth());
        }
    }
}
