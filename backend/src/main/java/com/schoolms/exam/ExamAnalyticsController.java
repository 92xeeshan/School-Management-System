package com.schoolms.exam;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.exam.dto.ClassAnalyticsDto;
import com.schoolms.exam.dto.ReportCardOptionsDto;
import com.schoolms.exam.dto.StudentTrendDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Exam Analytics")
@RequestMapping("/api/exam-analytics")
public class ExamAnalyticsController {

    private final ExamAnalyticsService examAnalyticsService;

    @Operation(summary = "Filter options for exam analytics")
    @PreAuthorize("hasAnyAuthority('EXAM_READ', 'EXAM_MANAGE', 'REPORT_CARD_READ')")
    @GetMapping("/options")
    public ApiResponse<ReportCardOptionsDto> options(@RequestParam(required = false) UUID studentId) {
        return ApiResponse.ok(examAnalyticsService.options(studentId));
    }

    @Operation(summary = "Class analytics from published report cards")
    @PreAuthorize("hasAnyAuthority('EXAM_READ', 'EXAM_MANAGE')")
    @GetMapping("/class")
    public ApiResponse<ClassAnalyticsDto> classAnalytics(
            @RequestParam UUID academicYearId,
            @RequestParam String examTerm,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) UUID sectionId) {
        return ApiResponse.ok(examAnalyticsService.classAnalytics(academicYearId, classId, sectionId, examTerm));
    }

    @Operation(summary = "Published exam trend for a student or linked child")
    @PreAuthorize("hasAnyAuthority('EXAM_READ', 'EXAM_MANAGE', 'REPORT_CARD_READ')")
    @GetMapping("/trend")
    public ApiResponse<StudentTrendDto> trend(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) UUID studentId) {
        return ApiResponse.ok(examAnalyticsService.studentTrend(academicYearId, studentId));
    }
}
