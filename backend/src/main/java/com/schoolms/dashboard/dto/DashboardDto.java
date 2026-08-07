package com.schoolms.dashboard.dto;

import com.schoolms.attendance.dto.AttendanceSummaryDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DashboardDto(
        long totalStudents,
        long totalTeachers,
        long totalClasses,
        long totalSections,
        long presentToday,
        long absentToday,
        long publishedNotices,
        long unreadNotices,
        BigDecimal feesCollected,
        List<MySectionDto> mySections,
        List<MyChildDto> myChildren,
        AttendanceSummaryDto myAttendance,
        BigDecimal feeBalance
) {
    public record MySectionDto(UUID id, String name, String className, long studentCount) {
    }

    public record MyChildDto(UUID id, String name, String admissionNo, String className, String sectionName) {
    }
}
