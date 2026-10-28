package com.schoolms.staff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NonTeachingStaffRepository extends JpaRepository<NonTeachingStaff, UUID> {

    List<NonTeachingStaff> findBySchoolIdOrderByFirstNameAsc(UUID schoolId);

    Optional<NonTeachingStaff> findByIdAndSchoolId(UUID id, UUID schoolId);

    Optional<NonTeachingStaff> findBySchoolIdAndUserId(UUID schoolId, UUID userId);

    boolean existsBySchoolIdAndEmployeeNo(UUID schoolId, String employeeNo);

    boolean existsBySchoolIdAndEmployeeNoAndIdNot(UUID schoolId, String employeeNo, UUID id);

    @Query("""
            select s.employeeNo from NonTeachingStaff s
            where s.schoolId = :schoolId and s.employeeNo like concat(:prefix, '%')
            """)
    List<String> findEmployeeNosByPrefix(@Param("schoolId") UUID schoolId, @Param("prefix") String prefix);
}
