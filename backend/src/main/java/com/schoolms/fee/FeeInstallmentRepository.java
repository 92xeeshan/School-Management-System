package com.schoolms.fee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeeInstallmentRepository extends JpaRepository<FeeInstallment, UUID> {

    List<FeeInstallment> findByStudentFeeAssignmentIdOrderByDueDateAsc(UUID assignmentId);

    Optional<FeeInstallment> findByIdAndSchoolId(UUID id, UUID schoolId);
}
