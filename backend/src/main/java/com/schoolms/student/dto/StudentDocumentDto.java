package com.schoolms.student.dto;

import com.schoolms.common.enums.StudentDocumentType;
import com.schoolms.student.StudentDocument;

import java.time.Instant;
import java.util.UUID;

public record StudentDocumentDto(
        UUID id,
        StudentDocumentType docType,
        String fileName,
        String contentType,
        String url,
        boolean verified,
        Instant createdAt
) {
    public static StudentDocumentDto from(StudentDocument doc, String url) {
        return new StudentDocumentDto(doc.getId(), doc.getDocType(), doc.getFileName(),
                doc.getContentType(), url, doc.isVerified(), doc.getCreatedAt());
    }
}
