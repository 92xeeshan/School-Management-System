package com.schoolms.expense.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ExpenseChartDto(
        int year,
        BigDecimal yearTotal,
        List<ExpenseMonthTotalDto> months,
        List<ExpenseCategoryTotalDto> categories
) {
    public record ExpenseMonthTotalDto(int month, BigDecimal total) {
    }

    public record ExpenseCategoryTotalDto(
            UUID categoryId,
            String code,
            String name,
            BigDecimal total,
            List<ExpenseMonthTotalDto> months
    ) {
    }
}
