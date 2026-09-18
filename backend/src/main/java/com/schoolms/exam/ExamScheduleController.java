package com.schoolms.exam;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.exam.dto.ExamScheduleDto;
import com.schoolms.exam.dto.ExamScheduleOptionsDto;
import com.schoolms.exam.dto.ExamScheduleRequest;
import com.schoolms.exam.dto.ScheduleStatusRequest;
import com.schoolms.exam.dto.ScheduleValidateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Exam Schedules")
@RequestMapping("/api/exam-schedules")
public class ExamScheduleController {

    private final ExamScheduleService examScheduleService;

    @Operation(summary = "Filter options for exam schedules")
    @PreAuthorize("hasAuthority('EXAM_MANAGE')")
    @GetMapping("/options")
    public ApiResponse<ExamScheduleOptionsDto> options() {
        return ApiResponse.ok(examScheduleService.options());
    }

    @Operation(summary = "List exam schedules")
    @PreAuthorize("hasAuthority('EXAM_MANAGE')")
    @GetMapping
    public ApiResponse<List<ExamScheduleDto>> list(
            @RequestParam UUID academicYearId,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) String room) {
        return ApiResponse.ok(examScheduleService.list(academicYearId, classId, sectionId, room));
    }

    @Operation(summary = "Validate exam schedule conflicts")
    @PreAuthorize("hasAuthority('EXAM_MANAGE')")
    @PostMapping("/validate")
    public ApiResponse<ScheduleValidateResponse> validate(
            @Valid @RequestBody ExamScheduleRequest request,
            @RequestParam(required = false) UUID excludeId) {
        return ApiResponse.ok(examScheduleService.validate(request, excludeId));
    }

    @Operation(summary = "Create an exam schedule")
    @PreAuthorize("hasAuthority('EXAM_MANAGE')")
    @PostMapping
    public ApiResponse<ExamScheduleDto> create(@Valid @RequestBody ExamScheduleRequest request) {
        return ApiResponse.ok(examScheduleService.create(request), "exam_schedule.created");
    }

    @Operation(summary = "Update an exam schedule")
    @PreAuthorize("hasAuthority('EXAM_MANAGE')")
    @PutMapping("/{id}")
    public ApiResponse<ExamScheduleDto> update(@PathVariable UUID id,
                                               @Valid @RequestBody ExamScheduleRequest request) {
        return ApiResponse.ok(examScheduleService.update(id, request), "exam_schedule.updated");
    }

    @Operation(summary = "Publish or unpublish an exam schedule")
    @PreAuthorize("hasAuthority('EXAM_MANAGE')")
    @PatchMapping("/{id}/status")
    public ApiResponse<ExamScheduleDto> updateStatus(@PathVariable UUID id,
                                                     @Valid @RequestBody ScheduleStatusRequest request) {
        return ApiResponse.ok(examScheduleService.updateStatus(id, request.status()), "exam_schedule.status_updated");
    }

    @Operation(summary = "Delete an exam schedule")
    @PreAuthorize("hasAuthority('EXAM_MANAGE')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        examScheduleService.delete(id);
        return ApiResponse.okMessage("exam_schedule.deleted");
    }
}
