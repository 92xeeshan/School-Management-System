package com.schoolms.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, UUID> {

    List<PayrollRun> findBySchoolIdOrderByYearDescMonthDesc(UUID schoolId);

    Optional<PayrollRun> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySchoolIdAndYearAndMonth(UUID schoolId, Short year, Short month);
}
