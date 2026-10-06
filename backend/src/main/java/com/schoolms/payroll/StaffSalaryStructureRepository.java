package com.schoolms.payroll;

import com.schoolms.common.enums.StaffType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffSalaryStructureRepository extends JpaRepository<StaffSalaryStructure, UUID> {

    List<StaffSalaryStructure> findBySchoolIdOrderByEffectiveFromDesc(UUID schoolId);

    Optional<StaffSalaryStructure> findByIdAndSchoolId(UUID id, UUID schoolId);

    Optional<StaffSalaryStructure> findFirstBySchoolIdAndStaffTypeAndStaffIdAndStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            UUID schoolId, StaffType staffType, UUID staffId, String status, LocalDate asOf);
}
