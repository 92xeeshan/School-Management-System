package com.schoolms.fee;

import com.schoolms.common.BaseEntity;
import com.schoolms.common.enums.PaymentMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fee_payment")
@Getter
@Setter
public class FeePayment extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "receipt_no", nullable = false)
    private String receiptNo;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "student_fee_assignment_id")
    private UUID studentFeeAssignmentId;

    @Column(name = "amount_paid", nullable = false)
    private BigDecimal amountPaid;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Column(name = "reference_no")
    private String referenceNo;

    private String remarks;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Column(name = "pdf_url")
    private String pdfUrl;
}
