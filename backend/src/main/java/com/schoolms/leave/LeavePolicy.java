package com.schoolms.leave;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "leave_policy")
@Getter
@Setter
public class LeavePolicy extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "staff_category", nullable = false, length = 20)
    private LeaveStaffCategory staffCategory = LeaveStaffCategory.ALL;

    @Column(nullable = false)
    private boolean active = true;
}
