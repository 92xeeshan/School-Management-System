package com.schoolms.notice;

import com.schoolms.common.enums.NoticeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoticeRepository extends JpaRepository<Notice, UUID> {

    Optional<Notice> findByIdAndSchoolId(UUID id, UUID schoolId);

    List<Notice> findBySchoolIdOrderByPublishAtDesc(UUID schoolId);

    List<Notice> findBySchoolIdAndStatusOrderByPublishAtDesc(UUID schoolId, NoticeStatus status);

    Page<Notice> findBySchoolId(UUID schoolId, Pageable pageable);

    Page<Notice> findBySchoolIdAndStatus(UUID schoolId, NoticeStatus status, Pageable pageable);

    Page<Notice> findBySchoolIdAndAuthorId(UUID schoolId, UUID authorId, Pageable pageable);

    @Query("""
            SELECT n FROM Notice n
            WHERE n.schoolId = :schoolId
              AND (:authorId IS NULL OR n.authorId = :authorId)
              AND (:status IS NULL OR n.status = :status)
              AND (:like IS NULL
                OR LOWER(n.title) LIKE :like
                OR LOWER(n.body) LIKE :like)
            """)
    Page<Notice> searchDirectory(@Param("schoolId") UUID schoolId,
                                 @Param("authorId") UUID authorId,
                                 @Param("status") NoticeStatus status,
                                 @Param("like") String like,
                                 Pageable pageable);

    long countBySchoolIdAndStatus(UUID schoolId, NoticeStatus status);

    /** Published notices that have not expired and are visible to the school. */
    List<Notice> findBySchoolIdAndStatusAndPublishAtLessThanEqualOrderByPublishAtDesc(
            UUID schoolId, NoticeStatus status, Instant now);
}
