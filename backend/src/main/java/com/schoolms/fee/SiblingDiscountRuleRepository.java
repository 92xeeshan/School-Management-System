package com.schoolms.fee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SiblingDiscountRuleRepository extends JpaRepository<SiblingDiscountRule, UUID> {

    List<SiblingDiscountRule> findBySchoolIdOrderBySiblingOrderAsc(UUID schoolId);

    List<SiblingDiscountRule> findBySchoolIdAndAcademicYearIdOrderBySiblingOrderAsc(UUID schoolId, UUID academicYearId);

    List<SiblingDiscountRule> findBySchoolIdAndAcademicYearIdAndStatusOrderBySiblingOrderAsc(
            UUID schoolId, UUID academicYearId, String status);

    Optional<SiblingDiscountRule> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySchoolIdAndAcademicYearIdAndSiblingOrderAndFeeCategoryId(
            UUID schoolId, UUID academicYearId, short siblingOrder, UUID feeCategoryId);

    boolean existsBySchoolIdAndAcademicYearIdAndSiblingOrderAndFeeCategoryIdAndIdNot(
            UUID schoolId, UUID academicYearId, short siblingOrder, UUID feeCategoryId, UUID id);
}
