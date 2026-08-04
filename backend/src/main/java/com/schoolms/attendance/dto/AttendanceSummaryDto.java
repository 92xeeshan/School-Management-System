package com.schoolms.attendance.dto;

import java.util.UUID;

public record AttendanceSummaryDto(
        UUID studentId,
        String studentName,
        long present,
        long absent,
        long late,
        long leave,
        long total,
        double percentage
) {
}
