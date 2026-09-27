package com.schoolms.notification;

import com.schoolms.common.api.PagedResponse;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.notification.dto.NotificationDto;
import com.schoolms.notification.dto.UnreadCountDto;
import com.schoolms.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public PagedResponse<NotificationDto> list(Boolean read, int page, int size) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UUID userId = SecurityUtils.currentUserId();
        int pageSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), pageSize);
        Page<Notification> result = read == null
                ? notificationRepository.findBySchoolIdAndRecipientIdOrderByCreatedAtDesc(schoolId, userId, pageable)
                : notificationRepository.findBySchoolIdAndRecipientIdAndReadOrderByCreatedAtDesc(
                        schoolId, userId, read, pageable);
        return PagedResponse.from(result.map(this::toDto));
    }

    @Transactional(readOnly = true)
    public UnreadCountDto unreadCount() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UUID userId = SecurityUtils.currentUserId();
        return new UnreadCountDto(notificationRepository.countBySchoolIdAndRecipientIdAndReadFalse(schoolId, userId));
    }

    @Transactional
    public NotificationDto markRead(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UUID userId = SecurityUtils.currentUserId();
        Notification notification = notificationRepository
                .findByIdAndSchoolIdAndRecipientId(id, schoolId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("notification", id));
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
        return toDto(notification);
    }

    @Transactional
    public UnreadCountDto markAllRead() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UUID userId = SecurityUtils.currentUserId();
        Instant now = Instant.now();
        List<Notification> unread = notificationRepository.findBySchoolIdAndRecipientIdAndReadFalse(schoolId, userId);
        for (Notification notification : unread) {
            notification.setRead(true);
            notification.setReadAt(now);
        }
        if (!unread.isEmpty()) {
            notificationRepository.saveAll(unread);
        }
        return new UnreadCountDto(0);
    }

    @Transactional
    public void notifyUsers(UUID schoolId, Collection<UUID> recipientIds, String title, String message,
                            String category, String actionUrl, String sourceKey) {
        if (schoolId == null || recipientIds == null || recipientIds.isEmpty()) {
            return;
        }
        Set<UUID> unique = new LinkedHashSet<>();
        for (UUID recipientId : recipientIds) {
            if (recipientId != null) {
                unique.add(recipientId);
            }
        }
        if (unique.isEmpty()) {
            return;
        }
        for (UUID recipientId : unique) {
            if (sourceKey != null
                    && notificationRepository.existsBySchoolIdAndRecipientIdAndSourceKey(schoolId, recipientId, sourceKey)) {
                continue;
            }
            Notification notification = new Notification();
            notification.setSchoolId(schoolId);
            notification.setRecipientId(recipientId);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setCategory(category);
            notification.setActionUrl(actionUrl);
            notification.setSourceKey(sourceKey);
            notification.setRead(false);
            notificationRepository.save(notification);
        }
    }

    private NotificationDto toDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getCategory(),
                notification.getActionUrl(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt());
    }
}
