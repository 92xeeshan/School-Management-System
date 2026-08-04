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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fee_structure")
@Getter
@Setter
public class FeeStructure extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeeFrequency frequency = FeeFrequency.MONTHLY;

    @Column(name = "due_day")
    private Short dueDay;

    @Column(name = "applicable_from")
    private LocalDate applicableFrom;

    @Column(name = "applicable_to")
    private LocalDate applicableTo;
}
