package com.schoolms.student;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.common.api.PagedResponse;
import com.schoolms.student.dto.AssignGuardianRequest;
import com.schoolms.student.dto.EnrollRequest;
import com.schoolms.student.dto.GuardianRequest;
import com.schoolms.student.dto.ImportResult;
import com.schoolms.student.dto.StudentDto;
import com.schoolms.student.dto.StudentGuardianDto;
import com.schoolms.student.dto.StudentListItemDto;
import com.schoolms.student.dto.StudentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Tag(name = "Students")
public class StudentController {

    private final StudentService studentService;

    @Operation(summary = "List/search students (paginated)")
    @PreAuthorize("hasAuthority('STUDENT_READ')")
    @GetMapping
    public ApiResponse<PagedResponse<StudentListItemDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String query) {
        Page<StudentListItemDto> result = studentService.list(page, Math.min(size, 100), query);
        return ApiResponse.ok(PagedResponse.from(result));
    }

    @Operation(summary = "List students enrolled in a section")
    @PreAuthorize("hasAuthority('STUDENT_READ')")
    @GetMapping("/by-section")
    public ApiResponse<List<StudentListItemDto>> bySection(
            @RequestParam UUID sectionId,
            @RequestParam UUID academicYearId) {
        return ApiResponse.ok(studentService.listBySection(sectionId, academicYearId));
    }

    @Operation(summary = "Get a student")
    @PreAuthorize("hasAuthority('STUDENT_READ')")
    @GetMapping("/{id}")
    public ApiResponse<StudentDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(studentService.get(id));
    }

    @Operation(summary = "Get a student's guardians")
    @PreAuthorize("hasAuthority('STUDENT_READ')")
    @GetMapping("/{id}/guardians")
    public ApiResponse<List<StudentGuardianDto>> guardians(@PathVariable UUID id) {
        return ApiResponse.ok(studentService.guardiansOf(id));
    }

    @Operation(summary = "Create a student")
    @PreAuthorize("hasAuthority('STUDENT_CREATE')")
    @PostMapping
    public ApiResponse<StudentDto> create(@Valid @RequestBody StudentRequest request) {
        return ApiResponse.ok(studentService.create(request));
    }

    @Operation(summary = "Update a student")
    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    @PutMapping("/{id}")
    public ApiResponse<StudentDto> update(@PathVariable UUID id, @Valid @RequestBody StudentRequest request) {
        return ApiResponse.ok(studentService.update(id, request));
    }

    @Operation(summary = "Enroll a student into a class/section for an academic year")
    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    @PostMapping("/{id}/enroll")
    public ApiResponse<Void> enroll(@PathVariable UUID id, @Valid @RequestBody EnrollRequest request) {
        studentService.enroll(id, request);
        return ApiResponse.okMessage("student.enrolled");
    }

    @Operation(summary = "Assign an existing guardian to a student")
    @PreAuthorize("hasAuthority('GUARDIAN_CREATE')")
    @PostMapping("/{id}/guardians")
    public ApiResponse<StudentGuardianDto> assignGuardian(@PathVariable UUID id,
                                                          @Valid @RequestBody AssignGuardianRequest request) {
        return ApiResponse.ok(studentService.assignGuardian(id, request.guardianId(),
                request.relationship(), request.primary()));
    }

    @Operation(summary = "Create a guardian and link to a student")
    @PreAuthorize("hasAuthority('GUARDIAN_CREATE')")
    @PostMapping("/{id}/guardians/new")
    public ApiResponse<StudentGuardianDto> createGuardian(@PathVariable UUID id,
                                                          @Valid @RequestBody GuardianRequest request) {
        return ApiResponse.ok(studentService.createAndLinkGuardian(id, request));
    }

    @Operation(summary = "Remove a guardian link from a student")
    @PreAuthorize("hasAuthority('GUARDIAN_UPDATE')")
    @DeleteMapping("/{id}/guardians/{guardianId}")
    public ApiResponse<Void> unlinkGuardian(@PathVariable UUID id, @PathVariable UUID guardianId) {
        studentService.unlinkGuardian(id, guardianId);
        return ApiResponse.okMessage("guardian.unlinked");
    }

    @Operation(summary = "Deactivate a student")
    @PreAuthorize("hasAuthority('STUDENT_DELETE')")
    @PatchMapping("/{id}/deactivate")
    public ApiResponse<Void> deactivate(@PathVariable UUID id) {
        studentService.deactivate(id);
        return ApiResponse.okMessage("student.deactivated");
    }

    @Operation(summary = "Bulk import students from CSV")
    @PreAuthorize("hasAuthority('STUDENT_IMPORT')")
    @PostMapping("/import")
    public ApiResponse<ImportResult> importCsv(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(studentService.importCsv(file));
    }
}
