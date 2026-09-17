package com.schoolms.staff;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.staff.dto.ClassTeacherRequest;
import com.schoolms.staff.dto.SectionRefDto;
import com.schoolms.staff.dto.StaffMemberDto;
import com.schoolms.staff.dto.StaffMemberRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Staff Management")
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService staffService;

    // ---- Teaching staff --------------------------------------------------

    @Operation(summary = "List teaching staff")
    @PreAuthorize("hasAuthority('STAFF_READ')")
    @GetMapping("/teachers")
    public ApiResponse<List<StaffMemberDto>> listTeachers() {
        return ApiResponse.ok(staffService.listTeachingStaff());
    }

    @Operation(summary = "Create a teaching staff member")
    @PreAuthorize("hasAuthority('STAFF_CREATE')")
    @PostMapping("/teachers")
    public ApiResponse<StaffMemberDto> createTeacher(@Valid @RequestBody StaffMemberRequest request) {
        return ApiResponse.ok(staffService.createTeachingStaff(request), "staff.created");
    }

    @Operation(summary = "Update a teaching staff member")
    @PreAuthorize("hasAuthority('STAFF_UPDATE')")
    @PutMapping("/teachers/{id}")
    public ApiResponse<StaffMemberDto> updateTeacher(@PathVariable UUID id,
                                                     @Valid @RequestBody StaffMemberRequest request) {
        return ApiResponse.ok(staffService.updateTeachingStaff(id, request), "staff.updated");
    }

    @Operation(summary = "Set or clear a teacher's class teacher assignment")
    @PreAuthorize("hasAuthority('STAFF_UPDATE')")
    @PutMapping("/teachers/{id}/class-teacher")
    public ApiResponse<StaffMemberDto> assignClassTeacher(@PathVariable UUID id,
                                                          @RequestBody ClassTeacherRequest request) {
        return ApiResponse.ok(staffService.assignClassTeacher(id, request), "staff.class_teacher_updated");
    }

    @Operation(summary = "Deactivate a teaching staff member")
    @PreAuthorize("hasAuthority('STAFF_DELETE')")
    @PatchMapping("/teachers/{id}/deactivate")
    public ApiResponse<Void> deactivateTeacher(@PathVariable UUID id) {
        staffService.deactivateTeachingStaff(id);
        return ApiResponse.okMessage("staff.deactivated");
    }

    // ---- Non-teaching staff ----------------------------------------------

    @Operation(summary = "List non-teaching staff")
    @PreAuthorize("hasAuthority('STAFF_READ')")
    @GetMapping("/non-teaching")
    public ApiResponse<List<StaffMemberDto>> listNonTeaching() {
        return ApiResponse.ok(staffService.listNonTeachingStaff());
    }

    @Operation(summary = "Create a non-teaching staff member")
    @PreAuthorize("hasAuthority('STAFF_CREATE')")
    @PostMapping("/non-teaching")
    public ApiResponse<StaffMemberDto> createNonTeaching(@Valid @RequestBody StaffMemberRequest request) {
        return ApiResponse.ok(staffService.createNonTeachingStaff(request), "staff.created");
    }

    @Operation(summary = "Update a non-teaching staff member")
    @PreAuthorize("hasAuthority('STAFF_UPDATE')")
    @PutMapping("/non-teaching/{id}")
    public ApiResponse<StaffMemberDto> updateNonTeaching(@PathVariable UUID id,
                                                         @Valid @RequestBody StaffMemberRequest request) {
        return ApiResponse.ok(staffService.updateNonTeachingStaff(id, request), "staff.updated");
    }

    @Operation(summary = "Deactivate a non-teaching staff member")
    @PreAuthorize("hasAuthority('STAFF_DELETE')")
    @PatchMapping("/non-teaching/{id}/deactivate")
    public ApiResponse<Void> deactivateNonTeaching(@PathVariable UUID id) {
        staffService.deactivateNonTeachingStaff(id);
        return ApiResponse.okMessage("staff.deactivated");
    }

    // ---- Reference data --------------------------------------------------

    @Operation(summary = "List sections with their current class teacher")
    @PreAuthorize("hasAuthority('STAFF_READ')")
    @GetMapping("/sections")
    public ApiResponse<List<SectionRefDto>> listSections() {
        return ApiResponse.ok(staffService.listSections());
    }
}
