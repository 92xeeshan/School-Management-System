package com.schoolms.student;

import com.schoolms.common.BaseEntity;
import com.schoolms.common.enums.StudentDocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "student_document")
@Getter
@Setter
public class StudentDocument extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false)
    private StudentDocumentType docType;

    @Column(name = "file_key", nullable = false)
    private String fileKey;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;
}
