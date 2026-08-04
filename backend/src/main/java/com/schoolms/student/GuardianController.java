package com.schoolms.student;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.student.dto.GuardianDto;
import com.schoolms.student.dto.GuardianRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/guardians")
@RequiredArgsConstructor
@Tag(name = "Guardians")
public class GuardianController {

    private final GuardianService guardianService;

    @Operation(summary = "List guardians in the current school")
    @PreAuthorize("hasAuthority('GUARDIAN_READ')")
    @GetMapping
    public ApiResponse<List<GuardianDto>> list() {
        return ApiResponse.ok(guardianService.list());
    }

    @Operation(summary = "Create a guardian")
    @PreAuthorize("hasAuthority('GUARDIAN_CREATE')")
    @PostMapping
    public ApiResponse<GuardianDto> create(@Valid @RequestBody GuardianRequest request) {
        return ApiResponse.ok(guardianService.create(request));
    }

    @Operation(summary = "Update a guardian")
    @PreAuthorize("hasAuthority('GUARDIAN_UPDATE')")
    @PatchMapping("/{id}")
    public ApiResponse<GuardianDto> update(@PathVariable UUID id, @Valid @RequestBody GuardianRequest request) {
        return ApiResponse.ok(guardianService.update(id, request));
    }
}
