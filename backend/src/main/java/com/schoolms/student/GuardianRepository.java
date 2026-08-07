package com.schoolms.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuardianRepository extends JpaRepository<Guardian, UUID> {

    Optional<Guardian> findByIdAndSchoolId(UUID id, UUID schoolId);

    Optional<Guardian> findBySchoolIdAndUserId(UUID schoolId, UUID userId);

    List<Guardian> findBySchoolId(UUID schoolId);
}
