package com.schoolms.award;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "award")
@Getter
@Setter
public class Award extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "academic_year_id")
    private UUID academicYearId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String category = "ACADEMIC";

    @Column(nullable = false)
    private String badge = "GOLD";

    private String description;

    @Column(name = "awarded_date", nullable = false)
    private LocalDate awardedDate;

    @Column(nullable = false)
    private int points;
}
