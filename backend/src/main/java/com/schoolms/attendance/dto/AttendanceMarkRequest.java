package com.schoolms.attendance.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AttendanceMarkRequest(
        @NotNull(message = "{validation.not_null}") UUID sectionId,
        @NotNull(message = "{validation.not_null}") UUID academicYearId,
        @NotNull(message = "{validation.not_null}") java.time.LocalDate attendanceDate,
        UUID subjectId,
        @NotNull(message = "{validation.not_null}") List<StudentAttendanceMark> marks
) {
    public record StudentAttendanceMark(
            @NotNull(message = "{validation.not_null}") UUID studentId,
            @NotNull(message = "{validation.not_null}") String status,
            String remark
    ) {
    }
}
