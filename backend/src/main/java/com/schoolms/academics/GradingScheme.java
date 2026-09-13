package com.schoolms.academics;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "grading_scheme")
@Getter
@Setter
public class GradingScheme extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(nullable = false)
    private String name;

    @Column(name = "academic_level", nullable = false)
    private String academicLevel = "PRIMARY";

    @Column(name = "exam_type", nullable = false)
    private String examType = "TERM";

    @Column(name = "scale_type", nullable = false)
    private String scaleType = "LETTER";

    @Column(name = "pass_marks", nullable = false)
    private BigDecimal passMarks;

    @Column(name = "pass_percent", nullable = false)
    private BigDecimal passPercent;

    @Column(name = "max_marks", nullable = false)
    private BigDecimal maxMarks;

    @Column(name = "evaluation_criteria")
    private String evaluationCriteria;

    @Column(nullable = false)
    private String status = "INACTIVE";
}
