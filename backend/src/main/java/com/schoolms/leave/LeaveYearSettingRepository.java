package com.schoolms.leave;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LeaveYearSettingRepository extends JpaRepository<LeaveYearSetting, UUID> {

    Optional<LeaveYearSetting> findBySchoolId(UUID schoolId);
}
