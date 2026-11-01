package com.schoolms.website;

import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.common.enums.EventVisibility;
import com.schoolms.common.enums.NoticeStatus;
import com.schoolms.common.enums.NoticeVisibility;
import com.schoolms.common.enums.SchoolStatus;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.event.SchoolEvent;
import com.schoolms.event.SchoolEventRepository;
import com.schoolms.notice.Notice;
import com.schoolms.notice.NoticeRepository;
import com.schoolms.school.School;
import com.schoolms.school.SchoolRepository;
import com.schoolms.security.SecurityUtils;
import com.schoolms.tenant.TenantBinder;
import com.schoolms.website.dto.PublicClassDto;
import com.schoolms.website.dto.PublicContactDto;
import com.schoolms.website.dto.PublicCopyDto;
import com.schoolms.website.dto.PublicCopyUpdateRequest;
import com.schoolms.website.dto.PublicEventDto;
import com.schoolms.website.dto.PublicNoticeDto;
import com.schoolms.website.dto.PublicSiteAdminDto;
import com.schoolms.website.dto.PublicSiteDto;
import com.schoolms.website.dto.PublicSiteUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicSiteService {

    private static final Set<String> LOCALES = Set.of("en", "hi", "ur");
    private static final int NOTICE_LIMIT = 5;
    private static final int EVENT_LIMIT = 8;

    private final SchoolRepository schoolRepository;
    private final PublicSiteRepository siteRepository;
    private final PublicSiteCopyRepository copyRepository;
    private final NoticeRepository noticeRepository;
    private final SchoolEventRepository eventRepository;
    private final SchoolClassRepository classRepository;
    private final TenantBinder tenantBinder;

    @Transactional(readOnly = true)
    public PublicSiteDto publicSite(String schoolCode, String locale) {
        School school = requireActiveSchool(schoolCode);
        tenantBinder.bind(school.getId());
        PublicSite site = siteRepository.findBySchoolId(school.getId())
                .orElseThrow(() -> new ResourceNotFoundException("public_site", schoolCode));
        if (!site.isPublished()) {
            throw new ResourceNotFoundException("public_site", schoolCode);
        }
        String resolved = resolveLocale(locale, school.getDefaultLocale());
        PublicCopyDto copy = copyDto(copyRepository.findBySchoolIdAndLocale(school.getId(), resolved)
                .or(() -> copyRepository.findBySchoolIdAndLocale(school.getId(), school.getDefaultLocale()))
                .or(() -> copyRepository.findBySchoolIdAndLocale(school.getId(), "en"))
                .orElse(null), resolved);
        Instant now = Instant.now();
        List<PublicNoticeDto> notices = noticeRepository
                .findBySchoolIdAndStatusAndPublishAtLessThanEqualOrderByPublishAtDesc(
                        school.getId(), NoticeStatus.PUBLISHED, now)
                .stream()
                .filter(notice -> notice.getVisibilityScope() == NoticeVisibility.SCHOOL_WIDE)
                .filter(notice -> notice.getExpiresAt() == null || notice.getExpiresAt().isAfter(now))
                .limit(NOTICE_LIMIT)
                .map(this::toNotice)
                .toList();
        LocalDate today = LocalDate.now();
        List<PublicEventDto> events = eventRepository.findUpcoming(school.getId(), today).stream()
                .filter(event -> event.getVisibilityScope() == EventVisibility.SCHOOL_WIDE)
                .limit(EVENT_LIMIT)
                .map(this::toEvent)
                .toList();
        List<PublicClassDto> classes = classRepository.findBySchoolIdOrderBySortOrderAsc(school.getId()).stream()
                .map(klass -> new PublicClassDto(klass.getId(), klass.getName()))
                .toList();
        return new PublicSiteDto(
                school.getId(),
                school.getCode(),
                school.getName(),
                school.getLogoUrl(),
                site.isPublished(),
                site.getHeroImageUrl(),
                site.getPrincipalName(),
                copy,
                new PublicContactDto(school.getAddress(), school.getPhone(), school.getEmail()),
                notices,
                events,
                classes);
    }

    @Transactional(readOnly = true)
    public PublicSiteAdminDto adminGet() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        PublicSite site = getOrCreate(schoolId);
        List<PublicCopyDto> copies = copyRepository.findBySchoolIdOrderByLocaleAsc(schoolId).stream()
                .map(row -> copyDto(row, row.getLocale()))
                .toList();
        return toAdminDto(site, copies);
    }

    @Transactional
    public PublicSiteAdminDto adminUpdate(PublicSiteUpdateRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        PublicSite site = getOrCreate(schoolId);
        if (request.published() != null) {
            site.setPublished(request.published());
        }
        if (request.heroImageUrl() != null) {
            site.setHeroImageUrl(blankToNull(request.heroImageUrl()));
        }
        if (request.principalName() != null) {
            site.setPrincipalName(blankToNull(request.principalName()));
        }
        site.setUpdatedBy(SecurityUtils.currentUserId());
        siteRepository.save(site);
        if (request.copies() != null) {
            for (PublicCopyUpdateRequest copyRequest : request.copies()) {
                String locale = normalizeLocale(copyRequest.locale());
                PublicSiteCopy copy = copyRepository.findBySchoolIdAndLocale(schoolId, locale)
                        .orElseGet(() -> {
                            PublicSiteCopy created = new PublicSiteCopy();
                            created.setSchoolId(schoolId);
                            created.setLocale(locale);
                            return created;
                        });
                copy.setTagline(blankToNull(copyRequest.tagline()));
                copy.setAbout(blankToNull(copyRequest.about()));
                copy.setPrincipalMessage(blankToNull(copyRequest.principalMessage()));
                copy.setFacilities(blankToNull(copyRequest.facilities()));
                copyRepository.save(copy);
            }
        }
        List<PublicCopyDto> copies = copyRepository.findBySchoolIdOrderByLocaleAsc(schoolId).stream()
                .map(row -> copyDto(row, row.getLocale()))
                .toList();
        return toAdminDto(site, copies);
    }

    School requireActiveSchool(String schoolCode) {
        if (schoolCode == null || schoolCode.isBlank()) {
            throw new ResourceNotFoundException("school", "code", schoolCode);
        }
        School school = schoolRepository.findByCode(schoolCode.trim().toUpperCase(Locale.ROOT))
                .or(() -> schoolRepository.findByCode(schoolCode.trim()))
                .orElseThrow(() -> new ResourceNotFoundException("school", "code", schoolCode));
        if (school.getStatus() != SchoolStatus.ACTIVE) {
            throw new ResourceNotFoundException("school", "code", schoolCode);
        }
        return school;
    }

    void bindPublishedSchool(School school) {
        tenantBinder.bind(school.getId());
        PublicSite site = siteRepository.findBySchoolId(school.getId())
                .orElseThrow(() -> new ResourceNotFoundException("public_site", school.getCode()));
        if (!site.isPublished()) {
            throw new ResourceNotFoundException("public_site", school.getCode());
        }
    }

    private PublicSite getOrCreate(UUID schoolId) {
        return siteRepository.findBySchoolId(schoolId).orElseGet(() -> {
            PublicSite created = new PublicSite();
            created.setSchoolId(schoolId);
            created.setPublished(false);
            return siteRepository.save(created);
        });
    }

    private PublicSiteAdminDto toAdminDto(PublicSite site, List<PublicCopyDto> copies) {
        return new PublicSiteAdminDto(
                site.getId(),
                site.getSchoolId(),
                site.isPublished(),
                site.getHeroImageUrl(),
                site.getPrincipalName(),
                copies);
    }

    private PublicCopyDto copyDto(PublicSiteCopy copy, String locale) {
        if (copy == null) {
            return new PublicCopyDto(locale, null, null, null, null);
        }
        return new PublicCopyDto(
                copy.getLocale(),
                copy.getTagline(),
                copy.getAbout(),
                copy.getPrincipalMessage(),
                copy.getFacilities());
    }

    private PublicNoticeDto toNotice(Notice notice) {
        return new PublicNoticeDto(
                notice.getId(),
                notice.getTitle(),
                notice.getBody(),
                notice.getPriority() == null ? null : notice.getPriority().name(),
                notice.getPublishAt());
    }

    private PublicEventDto toEvent(SchoolEvent event) {
        return new PublicEventDto(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventType() == null ? null : event.getEventType().name(),
                event.getStartDate(),
                event.getEndDate(),
                event.isAllDay(),
                event.getStartTime(),
                event.getEndTime(),
                event.getLocation());
    }

    private String resolveLocale(String requested, String schoolDefault) {
        if (requested != null && LOCALES.contains(requested)) {
            return requested;
        }
        if (schoolDefault != null && LOCALES.contains(schoolDefault)) {
            return schoolDefault;
        }
        return "en";
    }

    private String normalizeLocale(String locale) {
        if (locale == null || !LOCALES.contains(locale)) {
            throw new BusinessException("website.invalid_locale");
        }
        return locale;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
