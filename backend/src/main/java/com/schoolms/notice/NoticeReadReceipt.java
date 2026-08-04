package com.schoolms.notice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps the notice_read_receipt table (surrogate PK; read_at audit only).
 */
@Entity
@Table(name = "notice_read_receipt")
@Getter
@Setter
public class NoticeReadReceipt {

    @Id
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "notice_id", nullable = false)
    private UUID noticeId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "read_at", nullable = false)
    private Instant readAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (readAt == null) {
            readAt = Instant.now();
        }
    }
}
