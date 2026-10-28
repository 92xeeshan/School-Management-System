package com.schoolms.leave;

import com.schoolms.common.enums.StaffType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {

    List<LeaveBalance> findBySchoolIdAndStaffTypeAndStaffIdAndYearStart(
            UUID schoolId, StaffType staffType, UUID staffId, LocalDate yearStart);

    List<LeaveBalance> findBySchoolIdAndYearStart(UUID schoolId, LocalDate yearStart);

    Optional<LeaveBalance> findBySchoolIdAndStaffTypeAndStaffIdAndLeaveTypeIdAndYearStart(
            UUID schoolId, StaffType staffType, UUID staffId, UUID leaveTypeId, LocalDate yearStart);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from LeaveBalance b
            where b.schoolId = :schoolId
              and b.staffType = :staffType
              and b.staffId = :staffId
              and b.leaveTypeId = :leaveTypeId
              and b.yearStart = :yearStart
            """)
    Optional<LeaveBalance> lockByStaffAndType(
            @Param("schoolId") UUID schoolId,
            @Param("staffType") StaffType staffType,
            @Param("staffId") UUID staffId,
            @Param("leaveTypeId") UUID leaveTypeId,
            @Param("yearStart") LocalDate yearStart);
}
