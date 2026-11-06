package com.schoolms.academics;

import com.schoolms.common.BaseEntity;
import com.schoolms.common.enums.LookupKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "school_lookup")
@Getter
@Setter
public class SchoolLookup extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LookupKind kind;

    @Column(nullable = false)
    private String name;

    private String code;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private String status = "ACTIVE";
}
