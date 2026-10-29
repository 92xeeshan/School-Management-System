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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "leave_request")
@Getter
@Setter
public class LeaveRequest extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Enumerated(EnumType.STRING)
    @Column(name = "staff_type", nullable = false, length = 20)
    private StaffType staffType;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "leave_type_id", nullable = false)
    private UUID leaveTypeId;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(nullable = false, precision = 6, scale = 1)
    private BigDecimal days;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveRequestStatus status = LeaveRequestStatus.PENDING;

    @Column(name = "supporting_doc_key", length = 500)
    private String supportingDocKey;

    @Column(name = "supporting_doc_name")
    private String supportingDocName;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;
}
