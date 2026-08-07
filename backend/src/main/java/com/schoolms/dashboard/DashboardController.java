package com.schoolms.dashboard;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.dashboard.dto.DashboardDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Dashboard")
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Get role-specific dashboard aggregates")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    @GetMapping
    public ApiResponse<DashboardDto> get() {
        return ApiResponse.ok(dashboardService.get());
    }
}
