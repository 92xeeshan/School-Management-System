package com.schoolms.exam;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.exam.dto.MarksheetActionRequest;
import com.schoolms.exam.dto.MarksheetDto;
import com.schoolms.exam.dto.MarksheetExportRequest;
import com.schoolms.exam.dto.MarksheetOptionsDto;
import com.schoolms.exam.dto.MarksheetPublishRequest;
import com.schoolms.exam.dto.MarksheetStudentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Marksheets")
@RequestMapping("/api/marksheets")
public class MarksheetController {

    private final MarksheetService marksheetService;

    @Operation(summary = "Filter options for marksheets")
    @PreAuthorize("hasAuthority('MARKSHEET_READ')")
    @GetMapping("/options")
    public ApiResponse<MarksheetOptionsDto> options() {
        return ApiResponse.ok(marksheetService.options());
    }

    @Operation(summary = "List students eligible for marksheets")
    @PreAuthorize("hasAuthority('MARKSHEET_READ')")
    @GetMapping("/students")
    public ApiResponse<List<MarksheetStudentDto>> roster(
            @RequestParam UUID academicYearId,
            @RequestParam String examTerm,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(marksheetService.roster(academicYearId, classId, sectionId, examTerm, query, status));
    }

    @Operation(summary = "Current student or parent marksheet")
    @PreAuthorize("hasAuthority('MARKSHEET_READ')")
    @GetMapping("/mine")
    public ApiResponse<MarksheetDto> mine(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(defaultValue = "TERM") String examTerm) {
        return ApiResponse.ok(marksheetService.mine(academicYearId, examTerm));
    }

    @Operation(summary = "Submit marksheets for administrative approval")
    @PreAuthorize("hasAnyAuthority('EXAM_MARK', 'MARKSHEET_MANAGE')")
    @PostMapping("/submit")
    public ApiResponse<List<MarksheetStudentDto>> submit(@Valid @RequestBody MarksheetActionRequest request) {
        return ApiResponse.ok(marksheetService.submit(request));
    }

    @Operation(summary = "Approve and publish marksheets")
    @PreAuthorize("hasAuthority('MARKSHEET_MANAGE')")
    @PostMapping("/approve")
    public ApiResponse<List<MarksheetStudentDto>> approve(@Valid @RequestBody MarksheetActionRequest request) {
        return ApiResponse.ok(marksheetService.approve(request));
    }

    @Operation(summary = "Reject marksheets back to teachers")
    @PreAuthorize("hasAuthority('MARKSHEET_MANAGE')")
    @PostMapping("/reject")
    public ApiResponse<List<MarksheetStudentDto>> reject(@Valid @RequestBody MarksheetActionRequest request) {
        return ApiResponse.ok(marksheetService.reject(request));
    }

    @Operation(summary = "Publish or lock marksheets")
    @PreAuthorize("hasAuthority('MARKSHEET_MANAGE')")
    @PutMapping("/publish")
    public ApiResponse<List<MarksheetStudentDto>> publish(@Valid @RequestBody MarksheetPublishRequest request) {
        return ApiResponse.ok(marksheetService.publish(request));
    }

    @Operation(summary = "Marksheet for a student")
    @PreAuthorize("hasAuthority('MARKSHEET_READ')")
    @GetMapping("/{studentId}")
    public ApiResponse<MarksheetDto> get(
            @PathVariable UUID studentId,
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(defaultValue = "TERM") String examTerm) {
        return ApiResponse.ok(marksheetService.get(studentId, academicYearId, examTerm));
    }

    @Operation(summary = "Download marksheets as a single PDF")
    @PreAuthorize("hasAuthority('MARKSHEET_READ')")
    @PostMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@Valid @RequestBody MarksheetExportRequest request) {
        MarksheetService.ExportFile file = marksheetService.exportPdfFile(request);
        return ResponseEntity.ok()
                .headers(marksheetService.downloadHeaders(file.filename(), MediaType.APPLICATION_PDF_VALUE))
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, marksheetService.contentDisposition(file.filename()))
                .body(file.body());
    }

    @Operation(summary = "Download marksheets as a ZIP of PDFs")
    @PreAuthorize("hasAuthority('MARKSHEET_READ')")
    @PostMapping("/export/zip")
    public ResponseEntity<byte[]> exportZip(@Valid @RequestBody MarksheetExportRequest request) {
        byte[] body = marksheetService.exportZip(request);
        return ResponseEntity.ok()
                .headers(marksheetService.downloadHeaders("marksheets.zip", "application/zip"))
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, marksheetService.contentDisposition("marksheets.zip"))
                .body(body);
    }
}
