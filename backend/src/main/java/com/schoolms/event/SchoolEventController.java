package com.schoolms.event;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.event.dto.SchoolEventDto;
import com.schoolms.event.dto.SchoolEventRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Calendar Events")
@RequestMapping("/api/events")
public class SchoolEventController {

    private final SchoolEventService eventService;

    @Operation(summary = "List calendar events within a date range")
    @PreAuthorize("hasAuthority('EVENT_READ')")
    @GetMapping
    public ApiResponse<List<SchoolEventDto>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(eventService.list(from, to));
    }

    @Operation(summary = "List upcoming calendar events within the given number of days")
    @PreAuthorize("hasAuthority('EVENT_READ')")
    @GetMapping("/upcoming")
    public ApiResponse<List<SchoolEventDto>> upcoming(
            @RequestParam(required = false, defaultValue = "7") int days) {
        return ApiResponse.ok(eventService.upcoming(days));
    }

    @Operation(summary = "Create a calendar event")
    @PreAuthorize("hasAuthority('EVENT_MANAGE')")
    @PostMapping
    public ApiResponse<SchoolEventDto> create(@Valid @RequestBody SchoolEventRequest request) {
        return ApiResponse.ok(eventService.create(request));
    }

    @Operation(summary = "Update a calendar event")
    @PreAuthorize("hasAuthority('EVENT_MANAGE')")
    @PutMapping("/{id}")
    public ApiResponse<SchoolEventDto> update(@PathVariable UUID id,
                                              @Valid @RequestBody SchoolEventRequest request) {
        return ApiResponse.ok(eventService.update(id, request));
    }

    @Operation(summary = "Delete a calendar event")
    @PreAuthorize("hasAuthority('EVENT_MANAGE')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        eventService.delete(id);
        return ApiResponse.ok(null);
    }
}
