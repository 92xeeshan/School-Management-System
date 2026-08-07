package com.schoolms.academics;

import com.schoolms.common.BaseEntity;
import com.schoolms.common.enums.SubjectType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "subject")
@Getter
@Setter
public class Subject extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(nullable = false)
    private String name;

    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubjectType type = SubjectType.CORE;

    private String description;
}
