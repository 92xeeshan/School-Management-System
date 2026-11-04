package com.schoolms.student;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "student_previous_school")
@Getter
@Setter
public class StudentPreviousSchool extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "no_previous_school", nullable = false)
    private boolean noPreviousSchool;

    @Column(name = "school_name")
    private String schoolName;

    private String board;
    private String medium;

    @Column(name = "last_class")
    private String lastClass;

    @Column(name = "year_of_passing")
    private String yearOfPassing;

    private String percentage;

    @Column(name = "tc_number")
    private String tcNumber;

    @Column(name = "tc_date")
    private LocalDate tcDate;

    private String reason;
}
