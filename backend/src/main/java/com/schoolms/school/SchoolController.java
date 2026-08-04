package com.schoolms.school;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.school.dto.SchoolDto;
import com.schoolms.school.dto.SchoolRequest;
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
@RequestMapping("/api/schools")
@RequiredArgsConstructor
@Tag(name = "Schools")
public class SchoolController {

    private final SchoolService schoolService;

    @Operation(summary = "List all schools (super admin)")
    @PreAuthorize("hasAuthority('SCHOOL_READ')")
    @GetMapping
    public ApiResponse<List<SchoolDto>> list() {
        return ApiResponse.ok(schoolService.list());
    }

    @Operation(summary = "Get the current user's school")
    @GetMapping("/current")
    public ApiResponse<SchoolDto> current() {
        return ApiResponse.ok(schoolService.current());
    }

    @Operation(summary = "Get a school by id (super admin)")
    @PreAuthorize("hasAuthority('SCHOOL_READ')")
    @GetMapping("/{id}")
    public ApiResponse<SchoolDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(schoolService.get(id));
    }

    @Operation(summary = "Create a school (super admin)")
    @PreAuthorize("hasAuthority('SCHOOL_MANAGE')")
    @PostMapping
    public ApiResponse<SchoolDto> create(@Valid @RequestBody SchoolRequest request) {
        return ApiResponse.ok(schoolService.create(request));
    }

    @Operation(summary = "Update a school (super admin)")
    @PreAuthorize("hasAuthority('SCHOOL_MANAGE')")
    @PatchMapping("/{id}")
    public ApiResponse<SchoolDto> update(@PathVariable UUID id, @Valid @RequestBody SchoolRequest request) {
        return ApiResponse.ok(schoolService.update(id, request));
    }
}
