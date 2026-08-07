package com.schoolms.academics;

import com.schoolms.academics.dto.TimetableEntryDto;
import com.schoolms.academics.dto.TimetableEntryRequest;
import com.schoolms.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Timetable")
@RequestMapping("/api/timetable")
public class TimetableController {

    private final TimetableService timetableService;

    @Operation(summary = "List timetable entries for a section and academic year")
    @PreAuthorize("hasAnyAuthority('SECTION_READ','CLASS_READ')")
    @GetMapping
    public ApiResponse<List<TimetableEntryDto>> list(
            @RequestParam UUID sectionId,
            @RequestParam UUID academicYearId) {
        return ApiResponse.ok(timetableService.list(sectionId, academicYearId));
    }

    @Operation(summary = "Create a timetable entry")
    @PreAuthorize("hasAuthority('TIMETABLE_MANAGE')")
    @PostMapping
    public ApiResponse<TimetableEntryDto> create(@Valid @RequestBody TimetableEntryRequest request) {
        return ApiResponse.ok(timetableService.create(request));
    }

    @Operation(summary = "Delete a timetable entry")
    @PreAuthorize("hasAuthority('TIMETABLE_MANAGE')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        timetableService.delete(id);
        return ApiResponse.ok(null);
    }
}
