package com.schoolms.expense.dto;

import com.schoolms.common.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseRequest(
        @NotNull UUID categoryId,
        @NotNull @DecimalMin("0.00") BigDecimal amount,
        @NotNull LocalDate expenseDate,
        @Size(max = 120) String vendor,
        @Size(max = 500) String description,
        PaymentMethod paymentMethod
) {
}
