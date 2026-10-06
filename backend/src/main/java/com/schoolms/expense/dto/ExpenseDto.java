package com.schoolms.expense.dto;

import com.schoolms.common.enums.PaymentMethod;
import com.schoolms.expense.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseDto(
        UUID id,
        UUID categoryId,
        String categoryName,
        String categoryCode,
        BigDecimal amount,
        LocalDate expenseDate,
        String vendor,
        String description,
        PaymentMethod paymentMethod
) {
    public static ExpenseDto from(Expense expense, String categoryName, String categoryCode) {
        return new ExpenseDto(
                expense.getId(),
                expense.getCategoryId(),
                categoryName,
                categoryCode,
                expense.getAmount(),
                expense.getExpenseDate(),
                expense.getVendor(),
                expense.getDescription(),
                expense.getPaymentMethod());
    }
}
