package com.schoolms.expense;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    List<Expense> findBySchoolIdAndExpenseDateBetweenOrderByExpenseDateDesc(
            UUID schoolId, LocalDate from, LocalDate to);

    Optional<Expense> findByIdAndSchoolId(UUID id, UUID schoolId);
}
