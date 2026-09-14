package com.schoolms.exam;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.exam.dto.MarksGridDto;
import com.schoolms.exam.dto.MarksImportResult;
import com.schoolms.exam.dto.MarksLockRequest;
import com.schoolms.exam.dto.MarksOptionsDto;
import com.schoolms.exam.dto.MarksSaveRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Exam Marks")
@RequestMapping("/api/exam-marks")
public class ExamMarksController {

    private final ExamMarksService examMarksService;

    @Operation(summary = "Filter options for marks entry")
    @PreAuthorize("hasAnyAuthority('EXAM_READ', 'EXAM_MARK', 'EXAM_MANAGE')")
    @GetMapping("/options")
    public ApiResponse<MarksOptionsDto> options() {
        return ApiResponse.ok(examMarksService.options());
    }

    @Operation(summary = "Load the marks entry grid")
    @PreAuthorize("hasAnyAuthority('EXAM_READ', 'EXAM_MARK', 'EXAM_MANAGE')")
    @GetMapping
    public ApiResponse<MarksGridDto> grid(
            @RequestParam UUID academicYearId,
            @RequestParam UUID classId,
            @RequestParam UUID sectionId,
            @RequestParam UUID subjectId,
            @RequestParam String examTerm) {
        return ApiResponse.ok(examMarksService.grid(academicYearId, classId, sectionId, subjectId, examTerm));
    }

    @Operation(summary = "Save draft or submit marks")
    @PreAuthorize("hasAnyAuthority('EXAM_MARK', 'EXAM_MANAGE')")
    @PostMapping
    public ApiResponse<MarksGridDto> save(@Valid @RequestBody MarksSaveRequest request) {
        return ApiResponse.ok(examMarksService.save(request));
    }

    @Operation(summary = "Lock, unlock, or set the marks deadline")
    @PreAuthorize("hasAuthority('EXAM_MANAGE')")
    @PutMapping("/lock")
    public ApiResponse<MarksGridDto> lock(@Valid @RequestBody MarksLockRequest request) {
        return ApiResponse.ok(examMarksService.lock(request));
    }

    @Operation(summary = "Download a pre-filled marks template")
    @PreAuthorize("hasAnyAuthority('EXAM_READ', 'EXAM_MARK', 'EXAM_MANAGE')")
    @GetMapping("/template")
    public ResponseEntity<byte[]> template(
            @RequestParam UUID academicYearId,
            @RequestParam UUID classId,
            @RequestParam UUID sectionId,
            @RequestParam UUID subjectId,
            @RequestParam String examTerm,
            @RequestParam(defaultValue = "xlsx") String format) {
        boolean excel = !"csv".equalsIgnoreCase(format);
        byte[] body = examMarksService.exportTemplate(academicYearId, classId, sectionId, subjectId, examTerm, excel);
        return ResponseEntity.ok()
                .headers(examMarksService.downloadHeaders(excel))
                .contentType(MediaType.parseMediaType(examMarksService.templateContentType(excel)))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        examMarksService.contentDisposition(examMarksService.templateFilename(excel)))
                .body(body);
    }

    @Operation(summary = "Import marks from CSV or Excel")
    @PreAuthorize("hasAnyAuthority('EXAM_MARK', 'EXAM_MANAGE')")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MarksImportResult> importFile(
            @RequestParam UUID academicYearId,
            @RequestParam UUID classId,
            @RequestParam UUID sectionId,
            @RequestParam UUID subjectId,
            @RequestParam String examTerm,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(examMarksService.importFile(
                academicYearId, classId, sectionId, subjectId, examTerm, file));
    }
}
