package com.schoolms.expense;

import com.schoolms.common.enums.PaymentMethod;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.expense.dto.ExpenseCategoryDto;
import com.schoolms.expense.dto.ExpenseCategoryRequest;
import com.schoolms.expense.dto.ExpenseChartDto;
import com.schoolms.expense.dto.ExpenseChartDto.ExpenseCategoryTotalDto;
import com.schoolms.expense.dto.ExpenseChartDto.ExpenseMonthTotalDto;
import com.schoolms.expense.dto.ExpenseDto;
import com.schoolms.expense.dto.ExpenseRequest;
import com.schoolms.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseCategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public List<ExpenseCategoryDto> listCategories() {
        return categoryRepository.findBySchoolIdOrderByNameAsc(SecurityUtils.currentSchoolId())
                .stream().map(ExpenseCategoryDto::from).toList();
    }

    @Transactional
    public ExpenseCategoryDto createCategory(ExpenseCategoryRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        String code = request.code().trim().toUpperCase();
        if (categoryRepository.existsBySchoolIdAndCode(schoolId, code)) {
            throw new BusinessException("error.conflict");
        }
        ExpenseCategory category = new ExpenseCategory();
        category.setSchoolId(schoolId);
        category.setName(request.name().trim());
        category.setCode(code);
        category.setDescription(request.description());
        return ExpenseCategoryDto.from(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<ExpenseDto> list(Integer year, Integer month) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        LocalDate from;
        LocalDate to;
        if (year != null && month != null) {
            YearMonth ym = YearMonth.of(year, month);
            from = ym.atDay(1);
            to = ym.atEndOfMonth();
        } else if (year != null) {
            from = LocalDate.of(year, 1, 1);
            to = LocalDate.of(year, 12, 31);
        } else {
            int current = LocalDate.now().getYear();
            from = LocalDate.of(current, 1, 1);
            to = LocalDate.of(current, 12, 31);
        }
        Map<UUID, ExpenseCategory> categories = categoriesById(schoolId);
        return expenseRepository.findBySchoolIdAndExpenseDateBetweenOrderByExpenseDateDesc(schoolId, from, to)
                .stream()
                .map(row -> toDto(row, categories.get(row.getCategoryId())))
                .toList();
    }

    @Transactional
    public ExpenseDto create(ExpenseRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        ExpenseCategory category = categoryRepository.findByIdAndSchoolId(request.categoryId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("expense_category", request.categoryId()));
        Expense expense = new Expense();
        expense.setSchoolId(schoolId);
        expense.setCategoryId(category.getId());
        expense.setAmount(request.amount());
        expense.setExpenseDate(request.expenseDate());
        expense.setVendor(request.vendor());
        expense.setDescription(request.description());
        expense.setPaymentMethod(request.paymentMethod() == null ? PaymentMethod.CASH : request.paymentMethod());
        expense.setRecordedBy(SecurityUtils.currentUserId());
        return toDto(expenseRepository.save(expense), category);
    }

    @Transactional(readOnly = true)
    public ExpenseChartDto chart(int year) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year + 1, 1, 1);
        List<ExpenseCategory> categories = categoryRepository.findBySchoolIdOrderByNameAsc(schoolId);
        Map<UUID, ExpenseCategory> byId = new HashMap<>();
        for (ExpenseCategory category : categories) {
            byId.put(category.getId(), category);
        }

        BigDecimal[] monthTotals = new BigDecimal[13];
        for (int i = 1; i <= 12; i++) {
            monthTotals[i] = BigDecimal.ZERO;
        }
        Map<UUID, BigDecimal[]> byCategory = new LinkedHashMap<>();
        for (ExpenseCategory category : categories) {
            BigDecimal[] values = new BigDecimal[13];
            for (int i = 1; i <= 12; i++) {
                values[i] = BigDecimal.ZERO;
            }
            byCategory.put(category.getId(), values);
        }

        List<Expense> expenses = expenseRepository
                .findBySchoolIdAndExpenseDateBetweenOrderByExpenseDateDesc(schoolId, from, to.minusDays(1));
        for (Expense expense : expenses) {
            if (expense.getExpenseDate() == null) {
                continue;
            }
            int month = expense.getExpenseDate().getMonthValue();
            BigDecimal amount = expense.getAmount() == null ? BigDecimal.ZERO : expense.getAmount();
            monthTotals[month] = monthTotals[month].add(amount);
            BigDecimal[] values = byCategory.computeIfAbsent(expense.getCategoryId(), id -> {
                BigDecimal[] empty = new BigDecimal[13];
                for (int i = 1; i <= 12; i++) {
                    empty[i] = BigDecimal.ZERO;
                }
                return empty;
            });
            values[month] = values[month].add(amount);
            byId.computeIfAbsent(expense.getCategoryId(), id -> {
                ExpenseCategory missing = new ExpenseCategory();
                missing.setId(id);
                missing.setCode("UNKNOWN");
                missing.setName("Unknown");
                return missing;
            });
        }

        List<ExpenseMonthTotalDto> months = new ArrayList<>();
        BigDecimal yearTotal = BigDecimal.ZERO;
        for (int month = 1; month <= 12; month++) {
            months.add(new ExpenseMonthTotalDto(month, monthTotals[month]));
            yearTotal = yearTotal.add(monthTotals[month]);
        }

        List<ExpenseCategoryTotalDto> categoryTotals = new ArrayList<>();
        for (Map.Entry<UUID, BigDecimal[]> entry : byCategory.entrySet()) {
            ExpenseCategory category = byId.get(entry.getKey());
            if (category == null) {
                continue;
            }
            BigDecimal categoryTotal = BigDecimal.ZERO;
            List<ExpenseMonthTotalDto> categoryMonths = new ArrayList<>();
            for (int month = 1; month <= 12; month++) {
                BigDecimal value = entry.getValue()[month];
                categoryMonths.add(new ExpenseMonthTotalDto(month, value));
                categoryTotal = categoryTotal.add(value);
            }
            if (categoryTotal.compareTo(BigDecimal.ZERO) == 0 && !categories.contains(category)) {
                continue;
            }
            categoryTotals.add(new ExpenseCategoryTotalDto(
                    category.getId(),
                    category.getCode(),
                    category.getName(),
                    categoryTotal,
                    categoryMonths));
        }
        return new ExpenseChartDto(year, yearTotal, months, categoryTotals);
    }

    private Map<UUID, ExpenseCategory> categoriesById(UUID schoolId) {
        Map<UUID, ExpenseCategory> map = new HashMap<>();
        for (ExpenseCategory category : categoryRepository.findBySchoolIdOrderByNameAsc(schoolId)) {
            map.put(category.getId(), category);
        }
        return map;
    }

    private static ExpenseDto toDto(Expense expense, ExpenseCategory category) {
        return ExpenseDto.from(
                expense,
                category == null ? null : category.getName(),
                category == null ? null : category.getCode());
    }
}
