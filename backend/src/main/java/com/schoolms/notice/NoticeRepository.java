package com.schoolms.notice;

import com.schoolms.common.enums.NoticeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoticeRepository extends JpaRepository<Notice, UUID> {

    Optional<Notice> findByIdAndSchoolId(UUID id, UUID schoolId);

    List<Notice> findBySchoolIdOrderByPublishAtDesc(UUID schoolId);

    List<Notice> findBySchoolIdAndStatusOrderByPublishAtDesc(UUID schoolId, NoticeStatus status);

    long countBySchoolIdAndStatus(UUID schoolId, NoticeStatus status);

    /** Published notices that have not expired and are visible to the school. */
    List<Notice> findBySchoolIdAndStatusAndPublishAtLessThanEqualOrderByPublishAtDesc(
            UUID schoolId, NoticeStatus status, Instant now);
}
