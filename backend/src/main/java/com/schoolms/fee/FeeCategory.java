package com.schoolms.fee;

import com.schoolms.common.BaseEntity;
import com.schoolms.common.enums.FeeFrequency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "fee_category")
@Getter
@Setter
public class FeeCategory extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;

    private String description;

    @Column(name = "is_optional", nullable = false)
    private boolean optional;

    @Column(name = "is_refundable", nullable = false)
    private boolean refundable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeeFrequency frequency = FeeFrequency.MONTHLY;

    @Column(nullable = false)
    private String status = "ACTIVE";
}
