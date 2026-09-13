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
@Table(name = "grade_boundary")
@Getter
@Setter
public class GradeBoundary {

    @Id
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "scheme_id", nullable = false)
    private UUID schemeId;

    @Column(nullable = false)
    private String label;

    @Column(name = "min_percent", nullable = false)
    private BigDecimal minPercent;

    @Column(name = "max_percent", nullable = false)
    private BigDecimal maxPercent;

    @Column(name = "gpa_value")
    private BigDecimal gpaValue;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
