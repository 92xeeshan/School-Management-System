package com.schoolms.syllabus;

import com.schoolms.common.BaseEntity;
import com.schoolms.common.enums.SyllabusMaterialKind;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "syllabus_material")
@Getter
@Setter
public class SyllabusMaterial extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "unit_id")
    private UUID unitId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyllabusMaterialKind kind = SyllabusMaterialKind.DOCUMENT;

    @Column(nullable = false)
    private String title;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Basic(fetch = FetchType.LAZY)
    private byte[] content;

    @Column(name = "external_url")
    private String externalUrl;

    @Column(name = "uploaded_by_user_id")
    private UUID uploadedByUserId;
}
