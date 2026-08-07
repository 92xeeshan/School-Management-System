package com.schoolms.academics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, UUID> {

    List<TeacherProfile> findBySchoolIdOrderByFirstNameAsc(UUID schoolId);

    Optional<TeacherProfile> findByIdAndSchoolId(UUID id, UUID schoolId);

    Optional<TeacherProfile> findBySchoolIdAndUserId(UUID schoolId, UUID userId);

    boolean existsBySchoolIdAndEmployeeNo(UUID schoolId, String employeeNo);
}
