package com.schoolms.attendance.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AttendanceRecordDto(
        UUID id,
        UUID studentId,
        String studentName,
        String admissionNo,
        UUID sectionId,
        LocalDate attendanceDate,
        String status,
        String remark
) {
}
