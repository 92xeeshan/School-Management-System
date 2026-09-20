package com.schoolms.exam;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.exam.dto.AdmitCardDto;
import com.schoolms.exam.dto.AdmitCardExportRequest;
import com.schoolms.exam.dto.AdmitCardOptionsDto;
import com.schoolms.exam.dto.AdmitCardStudentDto;
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
@Tag(name = "Admit Cards")
@RequestMapping("/api/admit-cards")
public class AdmitCardController {

    private final AdmitCardService admitCardService;

    @Operation(summary = "Filter options for admit cards")
    @PreAuthorize("hasAuthority('ADMIT_CARD_READ')")
    @GetMapping("/options")
    public ApiResponse<AdmitCardOptionsDto> options() {
        return ApiResponse.ok(admitCardService.options());
    }

    @Operation(summary = "List students eligible for admit cards")
    @PreAuthorize("hasAuthority('ADMIT_CARD_READ')")
    @GetMapping("/students")
    public ApiResponse<List<AdmitCardStudentDto>> roster(
            @RequestParam UUID academicYearId,
            @RequestParam String examTerm,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) UUID sectionId) {
        return ApiResponse.ok(admitCardService.roster(academicYearId, classId, sectionId, examTerm));
    }

    @Operation(summary = "Current student admit card")
    @PreAuthorize("hasAuthority('ADMIT_CARD_READ')")
    @GetMapping("/mine")
    public ApiResponse<AdmitCardDto> mine(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(defaultValue = "TERM") String examTerm) {
        return ApiResponse.ok(admitCardService.mine(academicYearId, examTerm));
    }

    @Operation(summary = "Admit card for a student")
    @PreAuthorize("hasAuthority('ADMIT_CARD_READ')")
    @GetMapping("/{studentId}")
    public ApiResponse<AdmitCardDto> get(
            @PathVariable UUID studentId,
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(defaultValue = "TERM") String examTerm) {
        return ApiResponse.ok(admitCardService.get(studentId, academicYearId, examTerm));
    }

    @Operation(summary = "Download admit cards as a single PDF")
    @PreAuthorize("hasAuthority('ADMIT_CARD_READ')")
    @PostMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@Valid @RequestBody AdmitCardExportRequest request) {
        byte[] body = admitCardService.exportPdf(request);
        return ResponseEntity.ok()
                .headers(admitCardService.downloadHeaders("admit-cards.pdf", MediaType.APPLICATION_PDF_VALUE))
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, admitCardService.contentDisposition("admit-cards.pdf"))
                .body(body);
    }

    @Operation(summary = "Download admit cards as a ZIP of PDFs")
    @PreAuthorize("hasAuthority('ADMIT_CARD_READ')")
    @PostMapping("/export/zip")
    public ResponseEntity<byte[]> exportZip(@Valid @RequestBody AdmitCardExportRequest request) {
        byte[] body = admitCardService.exportZip(request);
        return ResponseEntity.ok()
                .headers(admitCardService.downloadHeaders("admit-cards.zip", "application/zip"))
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, admitCardService.contentDisposition("admit-cards.zip"))
                .body(body);
    }
}
