package com.schoolms.exam;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "exam_mark")
@Getter
@Setter
public class ExamMark extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "exam_entry_id", nullable = false)
    private UUID examEntryId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    private BigDecimal theory;

    private BigDecimal practical;

    private BigDecimal assignment;

    @Column(name = "attendance_status", nullable = false)
    private String attendanceStatus = "PRESENT";

    @Column(length = 500)
    private String remarks;

    private BigDecimal total;

    private BigDecimal percentage;

    @Column(name = "grade_label")
    private String gradeLabel;

    @Column(nullable = false)
    private String status = "DRAFT";
}
