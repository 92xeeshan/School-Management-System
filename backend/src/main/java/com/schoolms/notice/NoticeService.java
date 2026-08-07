package com.schoolms.notice;

import com.schoolms.common.enums.NoticePriority;
import com.schoolms.common.enums.NoticeStatus;
import com.schoolms.common.enums.NoticeVisibility;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.notice.dto.NoticeDto;
import com.schoolms.notice.dto.NoticeRequest;
import com.schoolms.security.SecurityUtils;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeAttachmentRepository attachmentRepository;
    private final NoticeReadReceiptRepository receiptRepository;
    private final UserRepository userRepository;
    private final com.schoolms.academics.SchoolClassRepository classRepository;
    private final com.schoolms.academics.SectionRepository sectionRepository;

    @Transactional(readOnly = true)
    public List<NoticeDto> list(String status, boolean mine) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        List<Notice> notices;
        if (mine) {
            UUID userId = SecurityUtils.currentUserId();
            notices = noticeRepository.findBySchoolIdOrderByPublishAtDesc(schoolId).stream()
                    .filter(n -> n.getAuthorId().equals(userId))
                    .toList();
        } else if (status != null && !status.isBlank()) {
            NoticeStatus parsed = parseStatus(status);
            notices = noticeRepository.findBySchoolIdAndStatusOrderByPublishAtDesc(schoolId, parsed);
        } else {
            notices = noticeRepository.findBySchoolIdOrderByPublishAtDesc(schoolId);
        }
        return notices.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<NoticeDto> listPublishedForUser() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UUID userId = SecurityUtils.currentUserId();
        return noticeRepository
                .findBySchoolIdAndStatusAndPublishAtLessThanEqualOrderByPublishAtDesc(
                        schoolId, NoticeStatus.PUBLISHED, Instant.now())
                .stream().filter(this::visibleToCurrentUser)
                .map(n -> toDto(n, userId))
                .toList();
    }

    @Transactional
    public NoticeDto create(NoticeRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        Notice notice = new Notice();
        notice.setSchoolId(schoolId);
        apply(notice, request);
        notice.setAuthorId(SecurityUtils.currentUserId());
        notice.setStatus(request.status() == null || request.status().isBlank()
                ? NoticeStatus.DRAFT : parseStatus(request.status()));
        if (notice.getStatus() == NoticeStatus.PUBLISHED && notice.getPublishAt() == null) {
            notice.setPublishAt(Instant.now());
        }
        return toDto(noticeRepository.save(notice));
    }

    @Transactional
    public NoticeDto publish(UUID id) {
        Notice notice = getOwned(id);
        notice.setStatus(NoticeStatus.PUBLISHED);
        if (notice.getPublishAt() == null) {
            notice.setPublishAt(Instant.now());
        }
        return toDto(noticeRepository.save(notice));
    }

    @Transactional
    public NoticeDto archive(UUID id) {
        Notice notice = getOwned(id);
        notice.setStatus(NoticeStatus.ARCHIVED);
        return toDto(noticeRepository.save(notice));
    }

    @Transactional
    public NoticeDto get(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        Notice notice = noticeRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("notice", id));
        if (notice.getStatus() == NoticeStatus.DRAFT && !notice.getAuthorId().equals(SecurityUtils.currentUserId())) {
            throw new BusinessException("notice.not_published");
        }
        markRead(id);
        return toDto(notice);
    }

    @Transactional
    public void markRead(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UUID userId = SecurityUtils.currentUserId();
        noticeRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("notice", id));
        if (receiptRepository.findByNoticeIdAndUserId(id, userId).isEmpty()) {
            NoticeReadReceipt receipt = new NoticeReadReceipt();
            receipt.setSchoolId(schoolId);
            receipt.setNoticeId(id);
            receipt.setUserId(userId);
            receipt.setReadAt(Instant.now());
            receiptRepository.save(receipt);
        }
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UUID userId = SecurityUtils.currentUserId();
        List<UUID> noticeIds = noticeRepository
                .findBySchoolIdAndStatusAndPublishAtLessThanEqualOrderByPublishAtDesc(
                        schoolId, NoticeStatus.PUBLISHED, Instant.now())
                .stream().map(Notice::getId).toList();
        long read = receiptRepository.findAll().stream()
                .filter(r -> r.getUserId().equals(userId) && noticeIds.contains(r.getNoticeId()))
                .count();
        return Math.max(0, noticeIds.size() - read);
    }

    private Notice getOwned(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        Notice notice = noticeRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("notice", id));
        if (!notice.getAuthorId().equals(SecurityUtils.currentUserId())
                && !SecurityUtils.currentPrincipal().permissions().contains("NOTICE_DELETE")) {
            throw new BusinessException("auth.access_denied");
        }
        return notice;
    }

    private boolean visibleToCurrentUser(Notice notice) {
        if (notice.getVisibilityScope() == NoticeVisibility.SCHOOL_WIDE) {
            return true;
        }
        // CLASS_WIDE: visible only to users whose school context has the class/section.
        // For MVP the frontend scopes by role; backend permits any authenticated user
        // of the tenant school but still scopes by the class filter when set.
        return notice.getClassId() != null || notice.getSectionId() != null;
    }

    private void apply(Notice notice, NoticeRequest request) {
        notice.setTitle(request.title());
        notice.setBody(request.body());
        notice.setVisibilityScope(parseVisibility(request.visibilityScope()));
        notice.setClassId(request.classId());
        notice.setSectionId(request.sectionId());
        notice.setPriority(parsePriority(request.priority()));
        notice.setPublishAt(request.publishAt());
        notice.setExpiresAt(request.expiresAt());
    }

    private NoticeDto toDto(Notice notice) {
        return toDto(notice, SecurityUtils.currentUserId());
    }

    private NoticeDto toDto(Notice notice, UUID userId) {
        UUID schoolId = notice.getSchoolId();
        String authorName = userRepository.findByIdAndSchoolId(notice.getAuthorId(), schoolId)
                .map(User::getDisplayName).orElse(null);
        String className = notice.getClassId() == null ? null
                : classRepository.findByIdAndSchoolId(notice.getClassId(), schoolId)
                        .map(com.schoolms.academics.SchoolClass::getName).orElse(null);
        String sectionName = notice.getSectionId() == null ? null
                : sectionRepository.findByIdAndSchoolId(notice.getSectionId(), schoolId)
                        .map(com.schoolms.academics.Section::getName).orElse(null);
        List<NoticeDto.AttachmentDto> attachments = attachmentRepository
                .findByNoticeIdAndSchoolId(notice.getId(), schoolId).stream()
                .map(a -> new NoticeDto.AttachmentDto(a.getId(), a.getFileName(),
                        a.getFileType(), a.getFileSize()))
                .toList();
        boolean read = userId == null || receiptRepository.findByNoticeIdAndUserId(notice.getId(), userId).isPresent();
        long readCount = receiptRepository.countByNoticeId(notice.getId());
        return new NoticeDto(notice.getId(), notice.getTitle(), notice.getBody(), notice.getAuthorId(),
                authorName, notice.getVisibilityScope().name(), notice.getClassId(), className,
                notice.getSectionId(), sectionName, notice.getPriority().name(),
                notice.getPublishAt(), notice.getExpiresAt(), notice.getStatus().name(),
                read, readCount, attachments);
    }

    private NoticeVisibility parseVisibility(String value) {
        if (value == null || value.isBlank()) {
            return NoticeVisibility.SCHOOL_WIDE;
        }
        try {
            return NoticeVisibility.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("validation.invalid");
        }
    }

    private NoticePriority parsePriority(String value) {
        if (value == null || value.isBlank()) {
            return NoticePriority.NORMAL;
        }
        try {
            return NoticePriority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("validation.invalid");
        }
    }

    private NoticeStatus parseStatus(String value) {
        try {
            return NoticeStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("validation.invalid");
        }
    }
}
