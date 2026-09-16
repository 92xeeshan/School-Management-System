package com.schoolms.fee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeeInstallmentRepository extends JpaRepository<FeeInstallment, UUID> {

    List<FeeInstallment> findByStudentFeeAssignmentIdOrderByDueDateAsc(UUID assignmentId);

    Optional<FeeInstallment> findByIdAndSchoolId(UUID id, UUID schoolId);

    @Query("""
            select coalesce(sum(i.amountDue - i.amountPaid), 0) from FeeInstallment i
            where i.schoolId = :schoolId and i.dueDate < :today and i.amountDue > i.amountPaid
            """)
    BigDecimal sumOverdue(@Param("schoolId") UUID schoolId, @Param("today") LocalDate today);
}
