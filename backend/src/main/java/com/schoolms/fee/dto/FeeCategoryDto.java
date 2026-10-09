package com.schoolms.fee.dto;

import com.schoolms.fee.FeeCategory;

import java.util.UUID;

public record FeeCategoryDto(
        UUID id,
        String name,
        String code,
        String description,
        boolean optional,
        boolean refundable,
        String frequency,
        String status
) {
    public static FeeCategoryDto from(FeeCategory category) {
        return new FeeCategoryDto(
                category.getId(),
                category.getName(),
                category.getCode(),
                category.getDescription(),
                category.isOptional(),
                category.isRefundable(),
                category.getFrequency() == null ? "MONTHLY" : category.getFrequency().name(),
                category.getStatus());
    }
}
