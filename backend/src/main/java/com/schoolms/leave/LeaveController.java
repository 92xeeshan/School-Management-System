package com.schoolms.leave;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.common.api.PagedResponse;
import com.schoolms.leave.dto.AdjustLeaveBalanceRequest;
import com.schoolms.leave.dto.ApplyLeaveRequest;
import com.schoolms.leave.dto.AssignLeavePolicyRequest;
import com.schoolms.leave.dto.CancelLeaveRequest;
import com.schoolms.leave.dto.LeaveAssignmentDto;
import com.schoolms.leave.dto.LeaveBalanceDto;
import com.schoolms.leave.dto.LeaveDayCountDto;
import com.schoolms.leave.dto.LeavePolicyDto;
import com.schoolms.leave.dto.LeavePolicyRequest;
import com.schoolms.leave.dto.LeaveRequestDto;
import com.schoolms.leave.dto.LeaveTypeDto;
import com.schoolms.leave.dto.LeaveTypeRequest;
import com.schoolms.leave.dto.LeaveYearSettingDto;
import com.schoolms.leave.dto.LeaveYearSettingRequest;
import com.schoolms.leave.dto.RejectLeaveRequest;
import com.schoolms.leave.dto.StaffLeaveOptionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Leave")
@RequestMapping("/api/leave")
public class LeaveController {

    private final LeaveService leaveService;

    @Operation(summary = "List leave types")
    @PreAuthorize("hasAnyAuthority('LEAVE_READ','LEAVE_MANAGE')")
    @GetMapping("/types")
    public ApiResponse<List<LeaveTypeDto>> listTypes() {
        return ApiResponse.ok(leaveService.listTypes());
    }

    @Operation(summary = "Create a leave type")
    @PreAuthorize("hasAuthority('LEAVE_MANAGE')")
    @PostMapping("/types")
    public ApiResponse<LeaveTypeDto> createType(@Valid @RequestBody LeaveTypeRequest request) {
        return ApiResponse.ok(leaveService.createType(request), "leave.type_saved");
    }

    @Operation(summary = "Update a leave type")
    @PreAuthorize("hasAuthority('LEAVE_MANAGE')")
    @PutMapping("/types/{id}")
    public ApiResponse<LeaveTypeDto> updateType(@PathVariable UUID id, @Valid @RequestBody LeaveTypeRequest request) {
        return ApiResponse.ok(leaveService.updateType(id, request), "leave.type_saved");
    }

    @Operation(summary = "Deactivate a leave type")
    @PreAuthorize("hasAuthority('LEAVE_MANAGE')")
    @PostMapping("/types/{id}/deactivate")
    public ApiResponse<LeaveTypeDto> deactivateType(@PathVariable UUID id) {
        return ApiResponse.ok(leaveService.deactivateType(id), "leave.type_deactivated");
    }

    @Operation(summary = "List leave policies")
    @PreAuthorize("hasAnyAuthority('LEAVE_READ','LEAVE_MANAGE')")
    @GetMapping("/policies")
    public ApiResponse<List<LeavePolicyDto>> listPolicies() {
        return ApiResponse.ok(leaveService.listPolicies());
    }

    @Operation(summary = "Create a leave policy")
    @PreAuthorize("hasAuthority('LEAVE_MANAGE')")
    @PostMapping("/policies")
    public ApiResponse<LeavePolicyDto> createPolicy(@Valid @RequestBody LeavePolicyRequest request) {
        return ApiResponse.ok(leaveService.createPolicy(request), "leave.policy_saved");
    }

    @Operation(summary = "Update a leave policy")
    @PreAuthorize("hasAuthority('LEAVE_MANAGE')")
    @PutMapping("/policies/{id}")
    public ApiResponse<LeavePolicyDto> updatePolicy(@PathVariable UUID id, @Valid @RequestBody LeavePolicyRequest request) {
        return ApiResponse.ok(leaveService.updatePolicy(id, request), "leave.policy_saved");
    }

    @Operation(summary = "Get leave year setting")
    @PreAuthorize("hasAnyAuthority('LEAVE_READ','LEAVE_MANAGE')")
    @GetMapping("/year")
    public ApiResponse<LeaveYearSettingDto> yearSetting() {
        return ApiResponse.ok(leaveService.yearSetting());
    }

    @Operation(summary = "Update leave year setting")
    @PreAuthorize("hasAuthority('LEAVE_MANAGE')")
    @PutMapping("/year")
    public ApiResponse<LeaveYearSettingDto> updateYear(@Valid @RequestBody LeaveYearSettingRequest request) {
        return ApiResponse.ok(leaveService.updateYearSetting(request), "leave.year_saved");
    }

    @Operation(summary = "List policy assignments")
    @PreAuthorize("hasAuthority('LEAVE_MANAGE')")
    @GetMapping("/assignments")
    public ApiResponse<List<LeaveAssignmentDto>> listAssignments() {
        return ApiResponse.ok(leaveService.listAssignments());
    }

    @Operation(summary = "List staff that can be assigned a policy")
    @PreAuthorize("hasAuthority('LEAVE_MANAGE')")
    @GetMapping("/staff")
    public ApiResponse<List<StaffLeaveOptionDto>> listStaff() {
        return ApiResponse.ok(leaveService.listStaffOptions());
    }

    @Operation(summary = "Assign a policy to employees")
    @PreAuthorize("hasAuthority('LEAVE_MANAGE')")
    @PostMapping("/assignments")
    public ApiResponse<List<LeaveAssignmentDto>> assign(@Valid @RequestBody AssignLeavePolicyRequest request) {
        return ApiResponse.ok(leaveService.assignPolicy(request), "leave.policy_assigned");
    }

    @Operation(summary = "Current employee leave balances")
    @PreAuthorize("hasAuthority('LEAVE_READ')")
    @GetMapping("/balances/mine")
    public ApiResponse<List<LeaveBalanceDto>> myBalances() {
        return ApiResponse.ok(leaveService.myBalances());
    }

    @Operation(summary = "Leave balance report")
    @PreAuthorize("hasAuthority('LEAVE_MANAGE')")
    @GetMapping("/balances")
    public ApiResponse<List<LeaveBalanceDto>> balances() {
        return ApiResponse.ok(leaveService.balanceReport());
    }

    @Operation(summary = "Adjust an employee leave balance")
    @PreAuthorize("hasAuthority('LEAVE_MANAGE')")
    @PostMapping("/balances/adjust")
    public ApiResponse<LeaveBalanceDto> adjust(@Valid @RequestBody AdjustLeaveBalanceRequest request) {
        return ApiResponse.ok(leaveService.adjustBalance(request), "leave.balance_adjusted");
    }

    @Operation(summary = "Preview working-day count for a leave range")
    @PreAuthorize("hasAnyAuthority('LEAVE_READ','LEAVE_APPLY')")
    @GetMapping("/days")
    public ApiResponse<LeaveDayCountDto> previewDays(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(leaveService.previewDays(from, to));
    }

    @Operation(summary = "Apply for leave")
    @PreAuthorize("hasAuthority('LEAVE_APPLY')")
    @PostMapping(value = "/requests", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<LeaveRequestDto> applyJson(@Valid @RequestBody ApplyLeaveRequest request) {
        return ApiResponse.ok(leaveService.apply(request, null), "leave.applied");
    }

    @Operation(summary = "Apply for leave with a supporting document")
    @PreAuthorize("hasAuthority('LEAVE_APPLY')")
    @PostMapping(value = "/requests", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<LeaveRequestDto> apply(
            @RequestParam UUID leaveTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam String reason,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ApiResponse.ok(leaveService.apply(new ApplyLeaveRequest(leaveTypeId, fromDate, toDate, reason), file),
                "leave.applied");
    }

    @Operation(summary = "Current employee leave history")
    @PreAuthorize("hasAuthority('LEAVE_READ')")
    @GetMapping("/requests/mine")
    public ApiResponse<PagedResponse<LeaveRequestDto>> myRequests(
            @RequestParam(required = false) LeaveRequestStatus status,
            @RequestParam(required = false) UUID leaveTypeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(leaveService.myRequests(status, leaveTypeId, page, size));
    }

    @Operation(summary = "Staff leave request queue")
    @PreAuthorize("hasAnyAuthority('LEAVE_APPROVE','LEAVE_MANAGE')")
    @GetMapping("/requests")
    public ApiResponse<PagedResponse<LeaveRequestDto>> listRequests(
            @RequestParam(required = false) LeaveRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(leaveService.listRequests(status, page, size));
    }

    @Operation(summary = "Approve a leave request")
    @PreAuthorize("hasAuthority('LEAVE_APPROVE')")
    @PostMapping("/requests/{id}/approve")
    public ApiResponse<LeaveRequestDto> approve(@PathVariable UUID id) {
        return ApiResponse.ok(leaveService.approve(id), "leave.approved");
    }

    @Operation(summary = "Reject a leave request")
    @PreAuthorize("hasAuthority('LEAVE_APPROVE')")
    @PostMapping("/requests/{id}/reject")
    public ApiResponse<LeaveRequestDto> reject(@PathVariable UUID id, @Valid @RequestBody RejectLeaveRequest request) {
        return ApiResponse.ok(leaveService.reject(id, request), "leave.rejected");
    }

    @Operation(summary = "Cancel a leave request")
    @PreAuthorize("hasAnyAuthority('LEAVE_APPLY','LEAVE_MANAGE')")
    @PostMapping("/requests/{id}/cancel")
    public ApiResponse<LeaveRequestDto> cancel(@PathVariable UUID id, @Valid @RequestBody CancelLeaveRequest request) {
        return ApiResponse.ok(leaveService.cancel(id, request), "leave.cancelled");
    }

    @Operation(summary = "Who is on leave today")
    @PreAuthorize("hasAnyAuthority('LEAVE_APPROVE','LEAVE_MANAGE')")
    @GetMapping("/reports/today")
    public ApiResponse<List<LeaveRequestDto>> onLeaveToday() {
        return ApiResponse.ok(leaveService.onLeaveToday());
    }

    @Operation(summary = "Leave taken this year")
    @PreAuthorize("hasAuthority('LEAVE_MANAGE')")
    @GetMapping("/reports/taken")
    public ApiResponse<List<LeaveRequestDto>> taken() {
        return ApiResponse.ok(leaveService.leaveTaken());
    }

    @Operation(summary = "Export leave balances as Excel")
    @PreAuthorize("hasAuthority('LEAVE_MANAGE')")
    @GetMapping("/reports/balances.xlsx")
    public ResponseEntity<byte[]> exportBalances() {
        return leaveService.exportBalances();
    }

    @Operation(summary = "Export leave taken as Excel")
    @PreAuthorize("hasAuthority('LEAVE_MANAGE')")
    @GetMapping("/reports/taken.xlsx")
    public ResponseEntity<byte[]> exportTaken() {
        return leaveService.exportTaken();
    }
}
