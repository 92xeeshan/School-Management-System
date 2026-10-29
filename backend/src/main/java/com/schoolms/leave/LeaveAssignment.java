package com.schoolms.leave;

import com.schoolms.common.BaseEntity;
import com.schoolms.common.enums.StaffType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "leave_assignment")
@Getter
@Setter
public class LeaveAssignment extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Enumerated(EnumType.STRING)
    @Column(name = "staff_type", nullable = false, length = 20)
    private StaffType staffType;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @Column(name = "year_start", nullable = false)
    private LocalDate yearStart;

    @Column(name = "assigned_by")
    private UUID assignedBy;
}
