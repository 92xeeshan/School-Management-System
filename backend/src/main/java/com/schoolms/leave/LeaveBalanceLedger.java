package com.schoolms.leave;

import com.schoolms.common.enums.StaffType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "leave_balance_ledger")
@Getter
@Setter
public class LeaveBalanceLedger {

    @Id
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Enumerated(EnumType.STRING)
    @Column(name = "staff_type", nullable = false, length = 20)
    private StaffType staffType;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "leave_type_id", nullable = false)
    private UUID leaveTypeId;

    @Column(name = "year_start", nullable = false)
    private LocalDate yearStart;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private LeaveLedgerType entryType;

    @Column(nullable = false, precision = 6, scale = 1)
    private BigDecimal days;

    @Column(name = "request_id")
    private UUID requestId;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
