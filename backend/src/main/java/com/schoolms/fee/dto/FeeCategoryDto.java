package com.schoolms.fee.dto;

import com.schoolms.fee.FeeCategory;

import java.util.UUID;

public record FeeCategoryDto(
        UUID id,
        String name,
        String code,
        String description
) {
    public static FeeCategoryDto from(FeeCategory category) {
        return new FeeCategoryDto(category.getId(), category.getName(),
                category.getCode(), category.getDescription());
    }
}
