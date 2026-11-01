package com.schoolms.website;

import com.schoolms.academics.SchoolClass;
import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.common.api.PagedResponse;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.notification.NotificationTriggerService;
import com.schoolms.school.School;
import com.schoolms.security.SecurityUtils;
import com.schoolms.website.dto.EnquiryDto;
import com.schoolms.website.dto.SubmitEnquiryRequest;
import com.schoolms.website.dto.UpdateEnquiryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EnquiryService {

    private static final Pattern PHONE = Pattern.compile("^[0-9+\\-() ]{7,20}$");

    private final EnquiryRepository enquiryRepository;
    private final SchoolClassRepository classRepository;
    private final PublicSiteService publicSiteService;
    private final NotificationTriggerService notifications;

    @Transactional
    public EnquiryDto submit(String schoolCode, SubmitEnquiryRequest request) {
        School school = publicSiteService.requireActiveSchool(schoolCode);
        publicSiteService.bindPublishedSchool(school);
        UUID schoolId = school.getId();
        String parentName = required(request.parentName(), "enquiry.parent_required");
        String phone = required(request.phone(), "enquiry.phone_required");
        if (!PHONE.matcher(phone).matches()) {
            throw new BusinessException("enquiry.phone_invalid");
        }
        String email = blankToNull(request.email());
        String className = blankToNull(request.className());
        UUID classId = request.classId();
        if (classId != null) {
            SchoolClass klass = classRepository.findByIdAndSchoolId(classId, schoolId)
                    .orElseThrow(() -> new BusinessException("class.not_found"));
            classId = klass.getId();
            if (className == null) {
                className = klass.getName();
            }
        }
        Enquiry enquiry = new Enquiry();
        enquiry.setSchoolId(schoolId);
        enquiry.setParentName(parentName);
        enquiry.setPhone(phone);
        enquiry.setEmail(email);
        enquiry.setClassId(classId);
        enquiry.setClassName(className);
        enquiry.setMessage(blankToNull(request.message()));
        enquiry.setStatus(EnquiryStatus.NEW);
        Enquiry saved = enquiryRepository.save(enquiry);
        notifySafely(() -> notifications.onEnquirySubmitted(
                schoolId, saved.getParentName(), saved.getClassName(), saved.getPhone(), saved.getId()));
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<EnquiryDto> list(EnquiryStatus status, int page, int size) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        int pageSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), pageSize);
        Page<Enquiry> result = status == null
                ? enquiryRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId, pageable)
                : enquiryRepository.findBySchoolIdAndStatusOrderByCreatedAtDesc(schoolId, status, pageable);
        return PagedResponse.from(result.map(this::toDto));
    }

    @Transactional
    public EnquiryDto update(UUID id, UpdateEnquiryRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        Enquiry enquiry = enquiryRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("enquiry", id));
        if (request.status() != null) {
            enquiry.setStatus(EnquiryStatus.valueOf(request.status()));
        }
        if (request.notes() != null) {
            enquiry.setNotes(blankToNull(request.notes()));
        }
        enquiry.setReviewedBy(SecurityUtils.currentUserId());
        enquiry.setReviewedAt(Instant.now());
        return toDto(enquiryRepository.save(enquiry));
    }

    private EnquiryDto toDto(Enquiry enquiry) {
        return new EnquiryDto(
                enquiry.getId(),
                enquiry.getParentName(),
                enquiry.getPhone(),
                enquiry.getEmail(),
                enquiry.getClassId(),
                enquiry.getClassName(),
                enquiry.getMessage(),
                enquiry.getStatus().name(),
                enquiry.getNotes(),
                enquiry.getCreatedAt(),
                enquiry.getReviewedAt());
    }

    private String required(String value, String code) {
        String trimmed = blankToNull(value);
        if (trimmed == null) {
            throw new BusinessException(code);
        }
        return trimmed;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void notifySafely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ignored) {
            // A notification failure must not fail the enquiry.
        }
    }
}
