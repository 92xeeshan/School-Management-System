package com.schoolms.expense.dto;

import com.schoolms.expense.ExpenseCategory;

import java.util.UUID;

public record ExpenseCategoryDto(
        UUID id,
        String name,
        String code,
        String description
) {
    public static ExpenseCategoryDto from(ExpenseCategory category) {
        return new ExpenseCategoryDto(category.getId(), category.getName(),
                category.getCode(), category.getDescription());
    }
}
