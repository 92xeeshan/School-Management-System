package com.schoolms.notice;

import com.schoolms.common.BaseEntity;
import com.schoolms.common.enums.NoticePriority;
import com.schoolms.common.enums.NoticeStatus;
import com.schoolms.common.enums.NoticeVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notice")
@Getter
@Setter
public class Notice extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_scope", nullable = false)
    private NoticeVisibility visibilityScope = NoticeVisibility.SCHOOL_WIDE;

    @Column(name = "class_id")
    private UUID classId;

    @Column(name = "section_id")
    private UUID sectionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoticePriority priority = NoticePriority.NORMAL;

    @Column(name = "publish_at")
    private Instant publishAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoticeStatus status = NoticeStatus.DRAFT;
}
