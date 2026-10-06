package com.schoolms.expense;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.expense.dto.ExpenseCategoryDto;
import com.schoolms.expense.dto.ExpenseCategoryRequest;
import com.schoolms.expense.dto.ExpenseChartDto;
import com.schoolms.expense.dto.ExpenseDto;
import com.schoolms.expense.dto.ExpenseRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Expenses")
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    @Operation(summary = "List expense categories")
    @PreAuthorize("hasAuthority('EXPENSE_READ')")
    @GetMapping("/categories")
    public ApiResponse<List<ExpenseCategoryDto>> listCategories() {
        return ApiResponse.ok(expenseService.listCategories());
    }

    @Operation(summary = "Create an expense category")
    @PreAuthorize("hasAuthority('EXPENSE_MANAGE')")
    @PostMapping("/categories")
    public ApiResponse<ExpenseCategoryDto> createCategory(@Valid @RequestBody ExpenseCategoryRequest request) {
        return ApiResponse.ok(expenseService.createCategory(request), "expense.category_created");
    }

    @Operation(summary = "List expenses")
    @PreAuthorize("hasAuthority('EXPENSE_READ')")
    @GetMapping
    public ApiResponse<List<ExpenseDto>> list(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ApiResponse.ok(expenseService.list(year, month));
    }

    @Operation(summary = "Record an expense")
    @PreAuthorize("hasAuthority('EXPENSE_MANAGE')")
    @PostMapping
    public ApiResponse<ExpenseDto> create(@Valid @RequestBody ExpenseRequest request) {
        return ApiResponse.ok(expenseService.create(request), "expense.created");
    }

    @Operation(summary = "Monthly expense totals by category")
    @PreAuthorize("hasAuthority('EXPENSE_READ')")
    @GetMapping("/chart")
    public ApiResponse<ExpenseChartDto> chart(@RequestParam(required = false) Integer year) {
        int resolved = year == null ? LocalDate.now().getYear() : year;
        return ApiResponse.ok(expenseService.chart(resolved));
    }
}
