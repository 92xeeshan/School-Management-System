package com.schoolms.notice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NoticeAttachmentRepository extends JpaRepository<NoticeAttachment, UUID> {

    List<NoticeAttachment> findByNoticeIdAndSchoolId(UUID noticeId, UUID schoolId);
}
