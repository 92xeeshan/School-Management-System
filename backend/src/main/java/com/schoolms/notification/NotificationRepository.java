package com.schoolms.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findBySchoolIdAndRecipientIdOrderByCreatedAtDesc(
            UUID schoolId, UUID recipientId, Pageable pageable);

    Page<Notification> findBySchoolIdAndRecipientIdAndReadOrderByCreatedAtDesc(
            UUID schoolId, UUID recipientId, boolean read, Pageable pageable);

    long countBySchoolIdAndRecipientIdAndReadFalse(UUID schoolId, UUID recipientId);

    List<Notification> findBySchoolIdAndRecipientIdAndReadFalse(UUID schoolId, UUID recipientId);

    Optional<Notification> findByIdAndSchoolIdAndRecipientId(UUID id, UUID schoolId, UUID recipientId);

    boolean existsBySchoolIdAndRecipientIdAndSourceKey(UUID schoolId, UUID recipientId, String sourceKey);
}
