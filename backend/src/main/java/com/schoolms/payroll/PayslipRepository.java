package com.schoolms.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayslipRepository extends JpaRepository<Payslip, UUID> {

    List<Payslip> findByPayrollRunIdOrderByStaffNameAsc(UUID payrollRunId);

    List<Payslip> findBySchoolIdAndUserIdOrderByCreatedAtDesc(UUID schoolId, UUID userId);

    Optional<Payslip> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsByPayrollRunId(UUID payrollRunId);
}
