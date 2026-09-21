package com.schoolms.exam;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.exam.dto.ReportCardDto;
import com.schoolms.exam.dto.ReportCardExportRequest;
import com.schoolms.exam.dto.ReportCardOptionsDto;
import com.schoolms.exam.dto.ReportCardStudentDto;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Report Cards")
@RequestMapping("/api/report-cards")
public class ReportCardController {

    private final ReportCardService reportCardService;

    @Operation(summary = "Filter options for report cards")
    @PreAuthorize("hasAuthority('REPORT_CARD_READ')")
    @GetMapping("/options")
    public ApiResponse<ReportCardOptionsDto> options() {
        return ApiResponse.ok(reportCardService.options());
    }

    @Operation(summary = "List students eligible for report cards")
    @PreAuthorize("hasAuthority('REPORT_CARD_READ')")
    @GetMapping("/students")
    public ApiResponse<List<ReportCardStudentDto>> roster(
            @RequestParam UUID academicYearId,
            @RequestParam String examTerm,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) UUID sectionId) {
        return ApiResponse.ok(reportCardService.roster(academicYearId, classId, sectionId, examTerm));
    }

    @Operation(summary = "Current student or parent report card")
    @PreAuthorize("hasAuthority('REPORT_CARD_READ')")
    @GetMapping("/mine")
    public ApiResponse<ReportCardDto> mine(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(defaultValue = "TERM") String examTerm) {
        return ApiResponse.ok(reportCardService.mine(academicYearId, examTerm));
    }

    @Operation(summary = "Report card for a student")
    @PreAuthorize("hasAuthority('REPORT_CARD_READ')")
    @GetMapping("/{studentId}")
    public ApiResponse<ReportCardDto> get(
            @PathVariable UUID studentId,
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(defaultValue = "TERM") String examTerm) {
        return ApiResponse.ok(reportCardService.get(studentId, academicYearId, examTerm));
    }

    @Operation(summary = "Download report cards as a single PDF")
    @PreAuthorize("hasAuthority('REPORT_CARD_READ')")
    @PostMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@Valid @RequestBody ReportCardExportRequest request) {
        byte[] body = reportCardService.exportPdf(request);
        return ResponseEntity.ok()
                .headers(reportCardService.downloadHeaders("report-cards.pdf", MediaType.APPLICATION_PDF_VALUE))
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, reportCardService.contentDisposition("report-cards.pdf"))
                .body(body);
    }

    @Operation(summary = "Download report cards as a ZIP of PDFs")
    @PreAuthorize("hasAuthority('REPORT_CARD_READ')")
    @PostMapping("/export/zip")
    public ResponseEntity<byte[]> exportZip(@Valid @RequestBody ReportCardExportRequest request) {
        byte[] body = reportCardService.exportZip(request);
        return ResponseEntity.ok()
                .headers(reportCardService.downloadHeaders("report-cards.zip", "application/zip"))
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, reportCardService.contentDisposition("report-cards.zip"))
                .body(body);
    }
}
