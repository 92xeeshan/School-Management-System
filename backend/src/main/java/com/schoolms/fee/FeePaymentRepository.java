package com.schoolms.fee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeePaymentRepository extends JpaRepository<FeePayment, UUID> {

    List<FeePayment> findByStudentIdAndSchoolIdOrderByPaidAtDesc(UUID studentId, UUID schoolId);

    Optional<FeePayment> findByIdAndSchoolId(UUID id, UUID schoolId);

    Optional<FeePayment> findBySchoolIdAndReceiptNo(UUID schoolId, String receiptNo);

    @Query("select coalesce(sum(p.amountPaid), 0) from FeePayment p where p.schoolId = :schoolId")
    BigDecimal sumBySchoolId(@Param("schoolId") UUID schoolId);
}
