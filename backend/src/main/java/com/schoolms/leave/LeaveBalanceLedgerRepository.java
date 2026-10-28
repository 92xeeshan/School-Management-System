package com.schoolms.leave;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LeaveBalanceLedgerRepository extends JpaRepository<LeaveBalanceLedger, UUID> {
}
