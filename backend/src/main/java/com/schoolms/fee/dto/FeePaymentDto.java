package com.schoolms.fee.dto;

import com.schoolms.fee.FeePayment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FeePaymentDto(
        UUID id,
        String receiptNo,
        UUID studentId,
        String studentName,
        UUID studentFeeAssignmentId,
        BigDecimal amountPaid,
        Instant paidAt,
        String paymentMethod,
        String referenceNo,
        String remarks,
        String pdfUrl
) {
    public static FeePaymentDto from(FeePayment payment, String studentName) {
        return new FeePaymentDto(payment.getId(), payment.getReceiptNo(), payment.getStudentId(),
                studentName, payment.getStudentFeeAssignmentId(), payment.getAmountPaid(),
                payment.getPaidAt(), payment.getPaymentMethod().name(), payment.getReferenceNo(),
                payment.getRemarks(), payment.getPdfUrl());
    }
}
