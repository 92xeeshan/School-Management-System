package com.schoolms.notification;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.common.api.PagedResponse;
import com.schoolms.notification.dto.NotificationDto;
import com.schoolms.notification.dto.UnreadCountDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Notifications")
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "List notifications for the current user")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ApiResponse<PagedResponse<NotificationDto>> list(
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(notificationService.list(read, page, size));
    }

    @Operation(summary = "Get unread notification count")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountDto> unreadCount() {
        return ApiResponse.ok(notificationService.unreadCount());
    }

    @Operation(summary = "Mark a notification as read")
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationDto> markRead(@PathVariable UUID id) {
        return ApiResponse.ok(notificationService.markRead(id));
    }

    @Operation(summary = "Mark all notifications as read")
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/read-all")
    public ApiResponse<UnreadCountDto> markAllRead() {
        return ApiResponse.ok(notificationService.markAllRead());
    }
}
