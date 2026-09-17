package com.schoolms.staff;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NonTeachingStaffRepository extends JpaRepository<NonTeachingStaff, UUID> {

    List<NonTeachingStaff> findBySchoolIdOrderByFirstNameAsc(UUID schoolId);

    Optional<NonTeachingStaff> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySchoolIdAndEmployeeNo(UUID schoolId, String employeeNo);

    boolean existsBySchoolIdAndEmployeeNoAndIdNot(UUID schoolId, String employeeNo, UUID id);
}
