package com.schoolms.fee.dto;

import com.schoolms.fee.SiblingDiscountRule;

import java.math.BigDecimal;
import java.util.UUID;

public record SiblingDiscountRuleDto(
        UUID id,
        UUID academicYearId,
        String academicYearName,
        short siblingOrder,
        String discountType,
        BigDecimal discountValue,
        UUID feeCategoryId,
        String feeCategoryName,
        String status
) {
    public static SiblingDiscountRuleDto from(SiblingDiscountRule rule, String yearName, String categoryName) {
        return new SiblingDiscountRuleDto(
                rule.getId(),
                rule.getAcademicYearId(),
                yearName,
                rule.getSiblingOrder(),
                rule.getDiscountType(),
                rule.getDiscountValue(),
                rule.getFeeCategoryId(),
                categoryName,
                rule.getStatus());
    }
}
