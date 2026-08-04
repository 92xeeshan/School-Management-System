package com.schoolms.fee.dto;

import com.schoolms.fee.FeeInstallment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InstallmentDto(
        UUID id,
        UUID studentFeeAssignmentId,
        LocalDate dueDate,
        BigDecimal amountDue,
        BigDecimal amountPaid,
        String status,
        BigDecimal balance
) {
    public static InstallmentDto from(FeeInstallment installment) {
        return new InstallmentDto(installment.getId(), installment.getStudentFeeAssignmentId(),
                installment.getDueDate(), installment.getAmountDue(), installment.getAmountPaid(),
                installment.getStatus().name(),
                installment.getAmountDue().subtract(installment.getAmountPaid()));
    }
}
