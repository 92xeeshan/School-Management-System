package com.schoolms.academics;

import com.schoolms.academics.dto.AcademicYearDto;
import com.schoolms.academics.dto.AcademicYearRequest;
import com.schoolms.academics.dto.ClassDto;
import com.schoolms.academics.dto.ClassRequest;
import com.schoolms.academics.dto.ClassUpdateRequest;
import com.schoolms.academics.dto.SectionDto;
import com.schoolms.academics.dto.SectionRequest;
import com.schoolms.academics.dto.SubjectDto;
import com.schoolms.academics.dto.SubjectRequest;
import com.schoolms.academics.dto.TeacherDto;
import com.schoolms.academics.dto.TeacherRequest;
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
@Tag(name = "Academics")
public class AcademicsController {

    private final AcademicsService academicsService;

    // ---- Academic years -----------------------------------------------------
    @Operation(summary = "List academic years")
    @PreAuthorize("hasAuthority('CLASS_READ')")
    @GetMapping("/api/academic-years")
    public ApiResponse<List<AcademicYearDto>> listYears() {
        return ApiResponse.ok(academicsService.listYears());
    }

    @Operation(summary = "Create an academic year")
    @PreAuthorize("hasAuthority('CLASS_CREATE')")
    @PostMapping("/api/academic-years")
    public ApiResponse<AcademicYearDto> createYear(@Valid @RequestBody AcademicYearRequest request) {
        return ApiResponse.ok(academicsService.createYear(request));
    }

    // ---- Classes -------------------------------------------------------------
    @Operation(summary = "List classes")
    @PreAuthorize("hasAuthority('CLASS_READ')")
    @GetMapping("/api/classes")
    public ApiResponse<List<ClassDto>> listClasses() {
        return ApiResponse.ok(academicsService.listClasses());
    }

    @Operation(summary = "Create a class")
    @PreAuthorize("hasAuthority('CLASS_CREATE')")
    @PostMapping("/api/classes")
    public ApiResponse<ClassDto> createClass(@Valid @RequestBody ClassRequest request) {
        return ApiResponse.ok(academicsService.createClass(request));
    }

    @Operation(summary = "Update a class and optional section")
    @PreAuthorize("hasAuthority('CLASS_UPDATE')")
    @PutMapping("/api/classes/{id}")
    public ApiResponse<ClassDto> updateClass(@PathVariable UUID id,
                                             @Valid @RequestBody ClassUpdateRequest request) {
        return ApiResponse.ok(academicsService.updateClass(id, request));
    }

    @Operation(summary = "Delete a class")
    @PreAuthorize("hasAuthority('CLASS_UPDATE')")
    @DeleteMapping("/api/classes/{id}")
    public ApiResponse<Void> deleteClass(@PathVariable UUID id) {
        academicsService.deleteClass(id);
        return ApiResponse.okMessage("class.deleted");
    }

    // ---- Sections -------------------------------------------------------------
    @Operation(summary = "List sections (optionally by class)")
    @PreAuthorize("hasAuthority('SECTION_READ')")
    @GetMapping("/api/sections")
    public ApiResponse<List<SectionDto>> listSections(@RequestParam(required = false) UUID classId) {
        return ApiResponse.ok(academicsService.listSections(classId));
    }

    @Operation(summary = "Create a section")
    @PreAuthorize("hasAuthority('SECTION_CREATE')")
    @PostMapping("/api/sections")
    public ApiResponse<SectionDto> createSection(@Valid @RequestBody SectionRequest request) {
        return ApiResponse.ok(academicsService.createSection(request));
    }

    @Operation(summary = "Delete a section")
    @PreAuthorize("hasAnyAuthority('CLASS_UPDATE', 'SECTION_UPDATE')")
    @DeleteMapping("/api/sections/{id}")
    public ApiResponse<Void> deleteSection(@PathVariable UUID id) {
        academicsService.deleteSection(id);
        return ApiResponse.okMessage("section.deleted");
    }

    // ---- Subjects -------------------------------------------------------------
    @Operation(summary = "List subjects")
    @PreAuthorize("hasAuthority('SUBJECT_READ')")
    @GetMapping("/api/subjects")
    public ApiResponse<List<SubjectDto>> listSubjects() {
        return ApiResponse.ok(academicsService.listSubjects());
    }

    @Operation(summary = "Create a subject")
    @PreAuthorize("hasAuthority('SUBJECT_CREATE')")
    @PostMapping("/api/subjects")
    public ApiResponse<SubjectDto> createSubject(@Valid @RequestBody SubjectRequest request) {
        return ApiResponse.ok(academicsService.createSubject(request));
    }

    @Operation(summary = "Update a subject")
    @PreAuthorize("hasAuthority('SUBJECT_UPDATE')")
    @PutMapping("/api/subjects/{id}")
    public ApiResponse<SubjectDto> updateSubject(@PathVariable UUID id,
                                                 @Valid @RequestBody SubjectRequest request) {
        return ApiResponse.ok(academicsService.updateSubject(id, request));
    }

    // ---- Teachers ---------------------------------------------------------------
    @Operation(summary = "List teachers")
    @PreAuthorize("hasAuthority('CLASS_READ')")
    @GetMapping("/api/teachers")
    public ApiResponse<List<TeacherDto>> listTeachers() {
        return ApiResponse.ok(academicsService.listTeachers());
    }

    @Operation(summary = "Create a teacher")
    @PreAuthorize("hasAuthority('CLASS_CREATE')")
    @PostMapping("/api/teachers")
    public ApiResponse<TeacherDto> createTeacher(@Valid @RequestBody TeacherRequest request) {
        return ApiResponse.ok(academicsService.createTeacher(request));
    }

    @Operation(summary = "Get a teacher's sections by id (helper)")
    @PreAuthorize("hasAuthority('SECTION_READ')")
    @GetMapping("/api/teachers/{id}")
    public ApiResponse<TeacherDto> getTeacher(@PathVariable UUID id) {
        return ApiResponse.ok(academicsService.getTeacher(id));
    }
}
