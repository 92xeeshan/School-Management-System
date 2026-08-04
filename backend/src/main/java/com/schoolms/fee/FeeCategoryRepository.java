package com.schoolms.fee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeeCategoryRepository extends JpaRepository<FeeCategory, UUID> {

    List<FeeCategory> findBySchoolIdOrderByNameAsc(UUID schoolId);

    Optional<FeeCategory> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySchoolIdAndCode(UUID schoolId, String code);
}
