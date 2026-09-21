package com.schoolms.exam;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "report_card")
@Getter
@Setter
public class ReportCard extends BaseEntity {

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

    @Column(name = "teacher_comment", length = 300)
    private String teacherComment;

    @Column(name = "principal_comment", length = 300)
    private String principalComment;

    @Column(name = "behaviour_conduct", nullable = false)
    private String behaviourConduct = "GOOD";

    @Column(name = "behaviour_discipline", nullable = false)
    private String behaviourDiscipline = "GOOD";

    @Column(name = "behaviour_punctuality", nullable = false)
    private String behaviourPunctuality = "GOOD";

    @Column(name = "co_curricular", length = 300)
    private String coCurricular;

    @Column(nullable = false)
    private boolean published;
}
