package com.schoolms.fee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentFeeAssignmentRepository extends JpaRepository<StudentFeeAssignment, UUID> {

    List<StudentFeeAssignment> findByStudentIdAndSchoolId(UUID studentId, UUID schoolId);

    Optional<StudentFeeAssignment> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsByStudentIdAndFeeStructureId(UUID studentId, UUID feeStructureId);
}
