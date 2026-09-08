package com.schoolms.event;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.event.dto.EventDto;
import com.schoolms.event.dto.EventRequest;
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
@Tag(name = "Events")
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    @Operation(summary = "List calendar events visible to the current user")
    @PreAuthorize("hasAnyAuthority('EVENT_READ','STUDENT_READ','CLASS_READ','NOTICE_READ')")
    @GetMapping
    public ApiResponse<List<EventDto>> list(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String type) {
        return ApiResponse.ok(eventService.listVisible(from, to, type));
    }

    @Operation(summary = "Get upcoming events for dashboard widgets")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/upcoming")
    public ApiResponse<List<EventDto>> upcoming() {
        return ApiResponse.ok(eventService.upcoming());
    }

    @Operation(summary = "Create an academic event")
    @PreAuthorize("hasAuthority('EVENT_MANAGE')")
    @PostMapping
    public ApiResponse<EventDto> create(@Valid @RequestBody EventRequest request) {
        return ApiResponse.ok(eventService.create(request));
    }

    @Operation(summary = "Update an academic event")
    @PreAuthorize("hasAuthority('EVENT_MANAGE')")
    @PutMapping("/{id}")
    public ApiResponse<EventDto> update(@PathVariable UUID id, @Valid @RequestBody EventRequest request) {
        return ApiResponse.ok(eventService.update(id, request));
    }

    @Operation(summary = "Delete an academic event")
    @PreAuthorize("hasAuthority('EVENT_MANAGE')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        eventService.delete(id);
        return ApiResponse.ok(null);
    }
}
