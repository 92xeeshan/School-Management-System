package com.schoolms.leave;

import com.schoolms.common.enums.StaffType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    Optional<LeaveRequest> findByIdAndSchoolId(UUID id, UUID schoolId);

    List<LeaveRequest> findBySchoolIdAndStaffTypeAndStaffIdOrderByFromDateDesc(
            UUID schoolId, StaffType staffType, UUID staffId);

    List<LeaveRequest> findBySchoolIdAndStatusOrderByCreatedAtDesc(UUID schoolId, LeaveRequestStatus status);

    List<LeaveRequest> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId);

    @Query("""
            select r from LeaveRequest r
            where r.schoolId = :schoolId
              and r.staffType = :staffType
              and r.staffId = :staffId
              and r.status in :statuses
              and r.fromDate <= :toDate
              and r.toDate >= :fromDate
            """)
    List<LeaveRequest> findOverlapping(
            @Param("schoolId") UUID schoolId,
            @Param("staffType") StaffType staffType,
            @Param("staffId") UUID staffId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("statuses") Collection<LeaveRequestStatus> statuses);

    @Query("""
            select r from LeaveRequest r
            where r.schoolId = :schoolId
              and r.status = :status
              and r.fromDate <= :onDate
              and r.toDate >= :onDate
            order by r.staffType, r.staffId
            """)
    List<LeaveRequest> findOnLeave(
            @Param("schoolId") UUID schoolId,
            @Param("status") LeaveRequestStatus status,
            @Param("onDate") LocalDate onDate);

    boolean existsBySchoolIdAndStaffTypeAndStaffIdAndLeaveTypeIdAndFromDateAndToDateAndStatus(
            UUID schoolId, StaffType staffType, UUID staffId, UUID leaveTypeId,
            LocalDate fromDate, LocalDate toDate, LeaveRequestStatus status);
}
