package com.schoolms.fee.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FeeAssignmentDto(
        UUID id,
        UUID studentId,
        String studentName,
        String admissionNo,
        UUID feeStructureId,
        String feeStructureName,
        BigDecimal amount,
        String frequency,
        String discountType,
        BigDecimal discountAmount,
        String status
) {
}
