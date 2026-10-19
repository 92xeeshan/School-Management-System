package com.schoolms.syllabus;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.common.enums.SyllabusMaterialKind;
import com.schoolms.syllabus.dto.SyllabusChapterDto;
import com.schoolms.syllabus.dto.SyllabusChapterRequest;
import com.schoolms.syllabus.dto.SyllabusChapterStatusRequest;
import com.schoolms.syllabus.dto.SyllabusCourseDetailDto;
import com.schoolms.syllabus.dto.SyllabusCourseDto;
import com.schoolms.syllabus.dto.SyllabusCourseRequest;
import com.schoolms.syllabus.dto.SyllabusMaterialDto;
import com.schoolms.syllabus.dto.SyllabusMaterialFile;
import com.schoolms.syllabus.dto.SyllabusUnitDto;
import com.schoolms.syllabus.dto.SyllabusUnitRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequiredArgsConstructor
@Tag(name = "Syllabus")
@RequestMapping("/api/syllabus")
public class SyllabusController {

    private final SyllabusService syllabusService;

    // ---- Courses --------------------------------------------------------

    @Operation(summary = "List syllabus courses")
    @PreAuthorize("hasAuthority('SYLLABUS_READ')")
    @GetMapping("/courses")
    public ApiResponse<List<SyllabusCourseDto>> list(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) UUID teacherId,
            @RequestParam(required = false) String progressStatus,
            @RequestParam(required = false) String query) {
        return ApiResponse.ok(syllabusService.list(
                academicYearId, classId, sectionId, subjectId, teacherId, progressStatus, query));
    }

    @Operation(summary = "Get a syllabus course with units, chapters and materials")
    @PreAuthorize("hasAuthority('SYLLABUS_READ')")
    @GetMapping("/courses/{id}")
    public ApiResponse<SyllabusCourseDetailDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(syllabusService.get(id));
    }

    @Operation(summary = "Create a syllabus course")
    @PreAuthorize("hasAuthority('SYLLABUS_MANAGE')")
    @PostMapping("/courses")
    public ApiResponse<SyllabusCourseDto> create(@Valid @RequestBody SyllabusCourseRequest request) {
        return ApiResponse.ok(syllabusService.create(request), "syllabus.course_created");
    }

    @Operation(summary = "Update a syllabus course")
    @PreAuthorize("hasAuthority('SYLLABUS_MANAGE')")
    @PutMapping("/courses/{id}")
    public ApiResponse<SyllabusCourseDto> update(@PathVariable UUID id,
                                                 @Valid @RequestBody SyllabusCourseRequest request) {
        return ApiResponse.ok(syllabusService.update(id, request), "syllabus.course_updated");
    }

    @Operation(summary = "Delete a syllabus course")
    @PreAuthorize("hasAuthority('SYLLABUS_MANAGE')")
    @DeleteMapping("/courses/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        syllabusService.delete(id);
        return ApiResponse.okMessage("syllabus.course_deleted");
    }

    // ---- Units ----------------------------------------------------------

    @Operation(summary = "Add a unit to a syllabus course")
    @PreAuthorize("hasAuthority('SYLLABUS_MANAGE')")
    @PostMapping("/courses/{courseId}/units")
    public ApiResponse<SyllabusUnitDto> createUnit(@PathVariable UUID courseId,
                                                   @Valid @RequestBody SyllabusUnitRequest request) {
        return ApiResponse.ok(syllabusService.createUnit(courseId, request), "syllabus.unit_created");
    }

    @Operation(summary = "Update a syllabus unit")
    @PreAuthorize("hasAuthority('SYLLABUS_MANAGE')")
    @PutMapping("/units/{id}")
    public ApiResponse<SyllabusUnitDto> updateUnit(@PathVariable UUID id,
                                                   @Valid @RequestBody SyllabusUnitRequest request) {
        return ApiResponse.ok(syllabusService.updateUnit(id, request), "syllabus.unit_updated");
    }

    @Operation(summary = "Delete a syllabus unit")
    @PreAuthorize("hasAuthority('SYLLABUS_MANAGE')")
    @DeleteMapping("/units/{id}")
    public ApiResponse<Void> deleteUnit(@PathVariable UUID id) {
        syllabusService.deleteUnit(id);
        return ApiResponse.okMessage("syllabus.unit_deleted");
    }

    // ---- Chapters -------------------------------------------------------

    @Operation(summary = "Add a chapter to a syllabus unit")
    @PreAuthorize("hasAuthority('SYLLABUS_MANAGE')")
    @PostMapping("/units/{unitId}/chapters")
    public ApiResponse<SyllabusChapterDto> createChapter(@PathVariable UUID unitId,
                                                         @Valid @RequestBody SyllabusChapterRequest request) {
        return ApiResponse.ok(syllabusService.createChapter(unitId, request), "syllabus.chapter_created");
    }

    @Operation(summary = "Update a syllabus chapter")
    @PreAuthorize("hasAuthority('SYLLABUS_MANAGE')")
    @PutMapping("/chapters/{id}")
    public ApiResponse<SyllabusChapterDto> updateChapter(@PathVariable UUID id,
                                                         @Valid @RequestBody SyllabusChapterRequest request) {
        return ApiResponse.ok(syllabusService.updateChapter(id, request), "syllabus.chapter_updated");
    }

    @Operation(summary = "Update chapter completion status")
    @PreAuthorize("hasAuthority('SYLLABUS_MANAGE')")
    @PatchMapping("/chapters/{id}/status")
    public ApiResponse<SyllabusChapterDto> updateChapterStatus(
            @PathVariable UUID id,
            @Valid @RequestBody SyllabusChapterStatusRequest request) {
        return ApiResponse.ok(syllabusService.updateChapterStatus(id, request), "syllabus.chapter_updated");
    }

    @Operation(summary = "Delete a syllabus chapter")
    @PreAuthorize("hasAuthority('SYLLABUS_MANAGE')")
    @DeleteMapping("/chapters/{id}")
    public ApiResponse<Void> deleteChapter(@PathVariable UUID id) {
        syllabusService.deleteChapter(id);
        return ApiResponse.okMessage("syllabus.chapter_deleted");
    }

    // ---- Materials ------------------------------------------------------

    @Operation(summary = "Attach a course outline or learning material")
    @PreAuthorize("hasAuthority('SYLLABUS_MANAGE')")
    @PostMapping(value = "/courses/{courseId}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SyllabusMaterialDto> addMaterial(
            @PathVariable UUID courseId,
            @RequestParam(required = false) SyllabusMaterialKind kind,
            @RequestParam String title,
            @RequestParam(required = false) UUID unitId,
            @RequestParam(required = false) String externalUrl,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ApiResponse.ok(syllabusService.addMaterial(courseId, kind, title, unitId, externalUrl, file),
                "syllabus.material_added");
    }

    @Operation(summary = "Download a syllabus material")
    @PreAuthorize("hasAuthority('SYLLABUS_READ')")
    @GetMapping("/materials/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        SyllabusMaterialFile file = syllabusService.download(id);
        String filename = file.fileName() == null ? "material" : file.fileName();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }

    @Operation(summary = "Delete a syllabus material")
    @PreAuthorize("hasAuthority('SYLLABUS_MANAGE')")
    @DeleteMapping("/materials/{id}")
    public ApiResponse<Void> deleteMaterial(@PathVariable UUID id) {
        syllabusService.deleteMaterial(id);
        return ApiResponse.okMessage("syllabus.material_deleted");
    }
}
