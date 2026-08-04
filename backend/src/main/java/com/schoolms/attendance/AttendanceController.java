package com.schoolms.attendance;

import com.schoolms.attendance.dto.AttendanceMarkRequest;
import com.schoolms.attendance.dto.AttendanceRecordDto;
import com.schoolms.attendance.dto.AttendanceSessionDto;
import com.schoolms.attendance.dto.AttendanceSummaryDto;
import com.schoolms.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Attendance")
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Operation(summary = "List attendance sessions for a section in a date range")
    @PreAuthorize("hasAuthority('ATTENDANCE_READ')")
    @GetMapping("/sessions")
    public ApiResponse<List<AttendanceSessionDto>> listSessions(
            @RequestParam UUID sectionId,
            @RequestParam UUID academicYearId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(attendanceService.listSessions(sectionId, academicYearId, from, to));
    }

    @Operation(summary = "Mark attendance for a section on a date")
    @PreAuthorize("hasAuthority('ATTENDANCE_MARK')")
    @PostMapping("/mark")
    public ApiResponse<Void> mark(@Valid @RequestBody AttendanceMarkRequest request) {
        attendanceService.mark(request);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "Get attendance records for a section on a date")
    @PreAuthorize("hasAuthority('ATTENDANCE_READ')")
    @GetMapping("/section")
    public ApiResponse<List<AttendanceRecordDto>> getSectionRecords(
            @RequestParam UUID sectionId,
            @RequestParam UUID academicYearId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID subjectId) {
        return ApiResponse.ok(attendanceService.getSectionRecords(sectionId, academicYearId, date, subjectId));
    }

    @Operation(summary = "Get a student's attendance records")
    @PreAuthorize("hasAnyAuthority('ATTENDANCE_READ','STUDENT_READ')")
    @GetMapping("/students/{studentId}")
    public ApiResponse<List<AttendanceRecordDto>> getStudentRecords(
            @PathVariable UUID studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(attendanceService.getStudentRecords(studentId, from, to));
    }

    @Operation(summary = "Get a student's attendance summary")
    @PreAuthorize("hasAnyAuthority('ATTENDANCE_READ','STUDENT_READ')")
    @GetMapping("/students/{studentId}/summary")
    public ApiResponse<AttendanceSummaryDto> getStudentSummary(
            @PathVariable UUID studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(attendanceService.getStudentSummary(studentId, from, to));
    }
}
