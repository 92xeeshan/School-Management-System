package com.schoolms.expense;

import com.schoolms.TestSecurity;
import com.schoolms.common.enums.PaymentMethod;
import com.schoolms.expense.dto.ExpenseChartDto;
import com.schoolms.expense.dto.ExpenseRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock private ExpenseCategoryRepository categoryRepository;
    @Mock private ExpenseRepository expenseRepository;

    private ExpenseService expenseService;

    private final UUID schoolId = TestSecurity.SCHOOL_ID;
    private final UUID categoryId = UUID.fromString("a1000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseService(categoryRepository, expenseRepository);
        TestSecurity.loginAsAdmin();
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void createPersistsExpenseForSchool() {
        ExpenseCategory category = category();
        when(categoryRepository.findByIdAndSchoolId(categoryId, schoolId)).thenReturn(Optional.of(category));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = expenseService.create(new ExpenseRequest(
                categoryId,
                new BigDecimal("18500.00"),
                LocalDate.of(2026, 4, 8),
                "BESCOM",
                "April electricity",
                PaymentMethod.BANK_TRANSFER));

        assertEquals(categoryId, dto.categoryId());
        assertEquals("Utilities", dto.categoryName());
        assertEquals(new BigDecimal("18500.00"), dto.amount());
    }

    @Test
    void chartAggregatesByMonthAndCategory() {
        when(categoryRepository.findBySchoolIdOrderByNameAsc(schoolId)).thenReturn(List.of(category()));
        Expense april = expense(LocalDate.of(2026, 4, 8), new BigDecimal("18500.00"));
        Expense may = expense(LocalDate.of(2026, 5, 7), new BigDecimal("19200.00"));
        when(expenseRepository.findBySchoolIdAndExpenseDateBetweenOrderByExpenseDateDesc(any(), any(), any()))
                .thenReturn(List.of(april, may));

        ExpenseChartDto chart = expenseService.chart(2026);

        assertEquals(2026, chart.year());
        assertEquals(new BigDecimal("37700.00"), chart.yearTotal());
        assertEquals(new BigDecimal("18500.00"), chart.months().get(3).total());
        assertEquals(1, chart.categories().size());
        assertEquals("UTILITIES", chart.categories().getFirst().code());
    }

    private ExpenseCategory category() {
        ExpenseCategory category = new ExpenseCategory();
        category.setId(categoryId);
        category.setSchoolId(schoolId);
        category.setName("Utilities");
        category.setCode("UTILITIES");
        return category;
    }

    private Expense expense(LocalDate date, BigDecimal amount) {
        Expense expense = new Expense();
        expense.setCategoryId(categoryId);
        expense.setExpenseDate(date);
        expense.setAmount(amount);
        return expense;
    }
}
