package com.schoolms.notice;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.notice.dto.NoticeDto;
import com.schoolms.notice.dto.NoticeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
@Tag(name = "Notices")
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "List notices (optionally filtered by status or only mine)")
    @PreAuthorize("hasAnyAuthority('NOTICE_READ','STUDENT_READ')")
    @GetMapping
    public ApiResponse<List<NoticeDto>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "false") boolean mine) {
        return ApiResponse.ok(noticeService.list(status, mine));
    }

    @Operation(summary = "List published notices visible to the current user")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/published")
    public ApiResponse<List<NoticeDto>> listPublished() {
        return ApiResponse.ok(noticeService.listPublishedForUser());
    }

    @Operation(summary = "Get unread published notice count")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount() {
        return ApiResponse.ok(noticeService.unreadCount());
    }

    @Operation(summary = "Get a notice (marks it read)")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ApiResponse<NoticeDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(noticeService.get(id));
    }

    @Operation(summary = "Create a notice")
    @PreAuthorize("hasAuthority('NOTICE_CREATE')")
    @PostMapping
    public ApiResponse<NoticeDto> create(@Valid @RequestBody NoticeRequest request) {
        return ApiResponse.ok(noticeService.create(request));
    }

    @Operation(summary = "Publish a notice")
    @PreAuthorize("hasAuthority('NOTICE_PUBLISH')")
    @PostMapping("/{id}/publish")
    public ApiResponse<NoticeDto> publish(@PathVariable UUID id) {
        return ApiResponse.ok(noticeService.publish(id));
    }

    @Operation(summary = "Archive a notice")
    @PreAuthorize("hasAnyAuthority('NOTICE_PUBLISH','NOTICE_DELETE')")
    @PostMapping("/{id}/archive")
    public ApiResponse<NoticeDto> archive(@PathVariable UUID id) {
        return ApiResponse.ok(noticeService.archive(id));
    }

    @Operation(summary = "Mark a notice as read")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable UUID id) {
        noticeService.markRead(id);
        return ApiResponse.ok(null);
    }
}
