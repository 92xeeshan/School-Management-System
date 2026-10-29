package com.schoolms.leave;

import com.schoolms.common.enums.StaffType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveAssignmentRepository extends JpaRepository<LeaveAssignment, UUID> {

    Optional<LeaveAssignment> findBySchoolIdAndStaffTypeAndStaffId(UUID schoolId, StaffType staffType, UUID staffId);

    List<LeaveAssignment> findBySchoolIdAndPolicyId(UUID schoolId, UUID policyId);

    List<LeaveAssignment> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId);
}
