package com.schoolms.event;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.event.dto.EventOptionsDto;
import com.schoolms.event.dto.SchoolEventDto;
import com.schoolms.event.dto.SchoolEventRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String type) {
        return ApiResponse.ok(eventService.list(from, to, type));
    }

    @Operation(summary = "List upcoming calendar events")
    @PreAuthorize("hasAuthority('EVENT_READ')")
    @GetMapping("/upcoming")
    public ApiResponse<List<SchoolEventDto>> upcoming(
            @RequestParam(required = false, defaultValue = "30") int days) {
        return ApiResponse.ok(eventService.upcoming(days));
    }

    @Operation(summary = "Filter options for calendar events")
    @PreAuthorize("hasAuthority('EVENT_READ')")
    @GetMapping("/options")
    public ApiResponse<EventOptionsDto> options() {
        return ApiResponse.ok(eventService.options());
    }

    @Operation(summary = "Export yearly holiday list as PDF")
    @PreAuthorize("hasAuthority('EVENT_MANAGE')")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) Integer year) {
        byte[] body = eventService.exportHolidays(year);
        String filename = "holiday-calendar-" + (year == null ? LocalDate.now().getYear() : year) + ".pdf";
        return ResponseEntity.ok()
                .headers(eventService.downloadHeaders(filename, MediaType.APPLICATION_PDF_VALUE))
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, eventService.contentDisposition(filename))
                .body(body);
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
