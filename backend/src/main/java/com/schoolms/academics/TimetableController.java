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
import org.springframework.web.bind.annotation.PutMapping;
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

    @Operation(summary = "List timetable entries")
    @PreAuthorize("hasAnyAuthority('TIMETABLE_READ', 'CLASS_READ', 'SECTION_READ')")
    @GetMapping
    public ApiResponse<List<TimetableEntryDto>> list(
            @RequestParam UUID academicYearId,
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) UUID teacherId,
            @RequestParam(required = false) String room) {
        return ApiResponse.ok(timetableService.list(academicYearId, sectionId, teacherId, room));
    }

    @Operation(summary = "Create a timetable entry")
    @PreAuthorize("hasAuthority('TIMETABLE_MANAGE')")
    @PostMapping
    public ApiResponse<TimetableEntryDto> create(@Valid @RequestBody TimetableEntryRequest request) {
        return ApiResponse.ok(timetableService.create(request));
    }

    @Operation(summary = "Update a timetable entry")
    @PreAuthorize("hasAuthority('TIMETABLE_MANAGE')")
    @PutMapping("/{id}")
    public ApiResponse<TimetableEntryDto> update(@PathVariable UUID id,
                                                 @Valid @RequestBody TimetableEntryRequest request) {
        return ApiResponse.ok(timetableService.update(id, request));
    }

    @Operation(summary = "Delete a timetable entry")
    @PreAuthorize("hasAuthority('TIMETABLE_MANAGE')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        timetableService.delete(id);
        return ApiResponse.okMessage("timetable.deleted");
    }
}
