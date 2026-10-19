package com.schoolms.syllabus;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "syllabus_course")
@Getter
@Setter
public class SyllabusCourse extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "section_id")
    private UUID sectionId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "teacher_id")
    private UUID teacherId;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "target_completion_date")
    private LocalDate targetCompletionDate;

    @Column(nullable = false)
    private String status = "ACTIVE";
}
