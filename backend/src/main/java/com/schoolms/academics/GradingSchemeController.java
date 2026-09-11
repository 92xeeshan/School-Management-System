package com.schoolms.academics;

import com.schoolms.academics.dto.GradingSchemeDto;
import com.schoolms.academics.dto.GradingSchemeRequest;
import com.schoolms.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
@Tag(name = "Grading Schemes")
@RequestMapping("/api/grading-schemes")
public class GradingSchemeController {

    private final GradingSchemeService gradingSchemeService;

    @Operation(summary = "List grading schemes")
    @PreAuthorize("hasAnyAuthority('EXAM_READ', 'CLASS_READ')")
    @GetMapping
    public ApiResponse<List<GradingSchemeDto>> list(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) UUID classId) {
        return ApiResponse.ok(gradingSchemeService.list(academicYearId, classId));
    }

    @Operation(summary = "Create a grading scheme")
    @PreAuthorize("hasAuthority('EXAM_MANAGE')")
    @PostMapping
    public ApiResponse<GradingSchemeDto> create(@Valid @RequestBody GradingSchemeRequest request) {
        return ApiResponse.ok(gradingSchemeService.create(request));
    }

    @Operation(summary = "Update a grading scheme")
    @PreAuthorize("hasAuthority('EXAM_MANAGE')")
    @PutMapping("/{id}")
    public ApiResponse<GradingSchemeDto> update(@PathVariable UUID id,
                                                @Valid @RequestBody GradingSchemeRequest request) {
        return ApiResponse.ok(gradingSchemeService.update(id, request));
    }

    @Operation(summary = "Activate a grading scheme for its class")
    @PreAuthorize("hasAuthority('EXAM_MANAGE')")
    @PutMapping("/{id}/activate")
    public ApiResponse<GradingSchemeDto> activate(@PathVariable UUID id) {
        return ApiResponse.ok(gradingSchemeService.activate(id));
    }

    @Operation(summary = "Deactivate a grading scheme")
    @PreAuthorize("hasAuthority('EXAM_MANAGE')")
    @PutMapping("/{id}/deactivate")
    public ApiResponse<GradingSchemeDto> deactivate(@PathVariable UUID id) {
        return ApiResponse.ok(gradingSchemeService.deactivate(id));
    }
}
