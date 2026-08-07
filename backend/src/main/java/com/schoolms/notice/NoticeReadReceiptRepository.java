package com.schoolms.notice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NoticeReadReceiptRepository extends JpaRepository<NoticeReadReceipt, UUID> {

    Optional<NoticeReadReceipt> findByNoticeIdAndUserId(UUID noticeId, UUID userId);

    long countByNoticeId(UUID noticeId);
}
