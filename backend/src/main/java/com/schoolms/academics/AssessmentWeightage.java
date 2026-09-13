package com.schoolms.academics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "assessment_weightage")
@Getter
@Setter
public class AssessmentWeightage {

    @Id
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "scheme_id", nullable = false)
    private UUID schemeId;

    @Column(name = "assessment_type", nullable = false)
    private String assessmentType;

    @Column(name = "weight_percent", nullable = false)
    private BigDecimal weightPercent;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
