package com.schoolms.exam;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "exam_entry")
@Getter
@Setter
public class ExamEntry extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "section_id", nullable = false)
    private UUID sectionId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "exam_term", nullable = false)
    private String examTerm;

    @Column(name = "max_theory", nullable = false)
    private BigDecimal maxTheory = new BigDecimal("80");

    @Column(name = "max_practical", nullable = false)
    private BigDecimal maxPractical = BigDecimal.ZERO;

    @Column(name = "max_assignment", nullable = false)
    private BigDecimal maxAssignment = new BigDecimal("20");

    @Column(name = "entry_deadline")
    private LocalDate entryDeadline;

    @Column(nullable = false)
    private boolean locked = false;

    @Column(name = "manual_unlock", nullable = false)
    private boolean manualUnlock = false;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "locked_by")
    private UUID lockedBy;
}
