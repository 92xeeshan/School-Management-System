package com.schoolms.leave;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, UUID> {

    List<LeaveType> findBySchoolIdOrderByCodeAsc(UUID schoolId);

    Optional<LeaveType> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySchoolIdAndCodeIgnoreCase(UUID schoolId, String code);

    boolean existsBySchoolIdAndCodeIgnoreCaseAndIdNot(UUID schoolId, String code, UUID id);
}
