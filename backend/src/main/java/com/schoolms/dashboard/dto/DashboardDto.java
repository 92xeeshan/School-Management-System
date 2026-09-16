package com.schoolms.dashboard.dto;

import com.schoolms.attendance.dto.AttendanceSummaryDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DashboardDto(
        long totalStudents,
        long totalTeachers,
        long totalStaff,
        long totalClasses,
        long totalSections,
        long totalAwards,
        long presentToday,
        long absentToday,
        long publishedNotices,
        long unreadNotices,
        BigDecimal feesCollected,
        BigDecimal feesOverdue,
        GenderBreakdownDto gender,
        List<AttendanceTrendPointDto> attendanceTrend,
        List<StarStudentDto> starStudents,
        List<MySectionDto> mySections,
        List<MyChildDto> myChildren,
        AttendanceSummaryDto myAttendance,
        BigDecimal feeBalance,
        long pendingGrading
) {
    public record GenderBreakdownDto(long boys, long girls, long other, long total,
                                     double boysPercent, double girlsPercent, double otherPercent) {
    }

    public record AttendanceTrendPointDto(LocalDate date, long present, long absent, long late,
                                          long leave, long total, double percentage) {
    }

    public record StarStudentDto(UUID studentId, String name, String admissionNo, String className,
                                 String sectionName, String achievement, String category, String badge,
                                 int points, LocalDate awardedDate) {
    }

    public record MySectionDto(UUID id, String name, String className, long studentCount,
                               Double averagePercentage) {
    }

    public record MyChildDto(UUID id, String name, String admissionNo, String className, String sectionName) {
    }
}
