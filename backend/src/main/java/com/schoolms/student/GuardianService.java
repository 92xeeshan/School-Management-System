package com.schoolms.student;

import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.common.enums.GuardianRelationship;
import com.schoolms.security.SecurityUtils;
import com.schoolms.student.dto.GuardianDto;
import com.schoolms.student.dto.GuardianRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuardianService {

    private final GuardianRepository guardianRepository;

    @Transactional(readOnly = true)
    public List<GuardianDto> list() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        return guardianRepository.findBySchoolId(schoolId).stream()
                .map(GuardianDto::from)
                .toList();
    }

    @Transactional
    public GuardianDto create(GuardianRequest request) {
        Guardian guardian = new Guardian();
        apply(guardian, request);
        guardian.setSchoolId(SecurityUtils.currentSchoolId());
        return GuardianDto.from(guardianRepository.save(guardian));
    }

    @Transactional
    public GuardianDto update(UUID id, GuardianRequest request) {
        Guardian guardian = guardianRepository
                .findByIdAndSchoolId(id, SecurityUtils.currentSchoolId())
                .orElseThrow(() -> ResourceNotFoundException.of("guardian", id));
        apply(guardian, request);
        return GuardianDto.from(guardianRepository.save(guardian));
    }

    private void apply(Guardian guardian, GuardianRequest request) {
        guardian.setFirstName(request.firstName());
        guardian.setLastName(request.lastName());
        guardian.setRelationship(parseRelationship(request.relationship()));
        guardian.setEmail(request.email());
        guardian.setPhone(request.phone());
        guardian.setOccupation(request.occupation());
        guardian.setUserId(request.userId());
    }

    private GuardianRelationship parseRelationship(String value) {
        if (value == null || value.isBlank()) {
            return GuardianRelationship.GUARDIAN;
        }
        try {
            return GuardianRelationship.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("validation.invalid");
        }
    }
}
