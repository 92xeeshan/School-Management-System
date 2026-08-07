package com.schoolms.academics;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "school_class")
@Getter
@Setter
public class SchoolClass extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(nullable = false)
    private String name;

    private String code;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
