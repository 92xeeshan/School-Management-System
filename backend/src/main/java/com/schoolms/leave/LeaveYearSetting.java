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
@Table(name = "leave_year_setting")
@Getter
@Setter
public class LeaveYearSetting extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveYearMode mode = LeaveYearMode.ACADEMIC;
}
