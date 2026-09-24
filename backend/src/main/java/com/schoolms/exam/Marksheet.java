package com.schoolms.exam;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "marksheet")
@Getter
@Setter
public class Marksheet extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "section_id", nullable = false)
    private UUID sectionId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "exam_term", nullable = false)
    private String examTerm;

    @Column(name = "serial_no", nullable = false, length = 80)
    private String serialNo;

    @Column(nullable = false)
    private boolean published;

    @Column(nullable = false)
    private boolean locked;

    @Column(name = "issued_at")
    private LocalDate issuedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "published_by")
    private UUID publishedBy;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "locked_by")
    private UUID lockedBy;
}
