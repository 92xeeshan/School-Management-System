package com.schoolms.certificate;

import com.schoolms.certificate.dto.CertificateIssuedDto;
import com.schoolms.certificate.dto.CertificateRequestDto;
import com.schoolms.certificate.dto.CertificateTemplateDto;
import com.schoolms.certificate.dto.CertificateTemplateRequest;
import com.schoolms.certificate.dto.GenerateCertificateRequest;
import com.schoolms.certificate.dto.RejectCertificateRequest;
import com.schoolms.certificate.dto.ReviewCertificateRequest;
import com.schoolms.certificate.dto.SubmitCertificateRequest;
import com.schoolms.common.api.ApiResponse;
import com.schoolms.common.api.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Certificates")
@RequestMapping("/api/certificates")
public class CertificateController {

    private final CertificateService certificateService;
    private final CertificateRequestService requestService;

    @Operation(summary = "List certificate templates")
    @PreAuthorize("hasAnyAuthority('CERTIFICATE_READ','CERTIFICATE_GENERATE','CERTIFICATE_MANAGE')")
    @GetMapping("/templates")
    public ApiResponse<List<CertificateTemplateDto>> listTemplates() {
        return ApiResponse.ok(certificateService.listTemplates());
    }

    @Operation(summary = "Create or update a certificate template")
    @PreAuthorize("hasAuthority('CERTIFICATE_MANAGE')")
    @PostMapping("/templates")
    public ApiResponse<CertificateTemplateDto> createTemplate(@Valid @RequestBody CertificateTemplateRequest request) {
        return ApiResponse.ok(certificateService.upsertTemplate(request), "certificate.template_saved");
    }

    @Operation(summary = "Update a certificate template")
    @PreAuthorize("hasAuthority('CERTIFICATE_MANAGE')")
    @PutMapping("/templates/{id}")
    public ApiResponse<CertificateTemplateDto> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody CertificateTemplateRequest request) {
        return ApiResponse.ok(certificateService.upsertTemplate(request), "certificate.template_saved");
    }

    @Operation(summary = "Generate a certificate for a student")
    @PreAuthorize("hasAuthority('CERTIFICATE_GENERATE')")
    @PostMapping("/generate")
    public ApiResponse<CertificateIssuedDto> generate(@Valid @RequestBody GenerateCertificateRequest request) {
        return ApiResponse.ok(certificateService.generate(request), "certificate.generated");
    }

    @Operation(summary = "Submit a student certificate request")
    @PreAuthorize("hasAuthority('CERTIFICATE_READ')")
    @PostMapping(value = "/requests", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<CertificateRequestDto> submitRequestJson(@Valid @RequestBody SubmitCertificateRequest request) {
        return ApiResponse.ok(requestService.submit(request, null), "certificate.request_submitted");
    }

    @Operation(summary = "Submit a student certificate request with supporting document")
    @PreAuthorize("hasAuthority('CERTIFICATE_READ')")
    @PostMapping(value = "/requests", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CertificateRequestDto> submitRequest(
            @RequestParam CertificateType type,
            @RequestParam String reason,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ApiResponse.ok(requestService.submit(new SubmitCertificateRequest(type, reason), file),
                "certificate.request_submitted");
    }

    @Operation(summary = "List current learner certificate requests")
    @PreAuthorize("hasAuthority('CERTIFICATE_READ')")
    @GetMapping("/requests/mine")
    public ApiResponse<List<CertificateRequestDto>> myRequests() {
        return ApiResponse.ok(requestService.mine());
    }

    @Operation(summary = "Staff certificate request queue")
    @PreAuthorize("hasAnyAuthority('CERTIFICATE_READ','CERTIFICATE_GENERATE','CERTIFICATE_APPROVE')")
    @GetMapping("/requests")
    public ApiResponse<PagedResponse<CertificateRequestDto>> listRequests(
            @RequestParam(required = false) CertificateType type,
            @RequestParam(required = false) CertificateRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(requestService.list(type, status, page, size));
    }

    @Operation(summary = "Class teacher forwards a request to principal")
    @PreAuthorize("hasAuthority('CERTIFICATE_GENERATE')")
    @PostMapping("/requests/{id}/review")
    public ApiResponse<CertificateRequestDto> review(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewCertificateRequest request) {
        return ApiResponse.ok(requestService.review(id, request), "certificate.request_forwarded");
    }

    @Operation(summary = "Cancel or send back a pending request")
    @PreAuthorize("hasAnyAuthority('CERTIFICATE_READ','CERTIFICATE_GENERATE')")
    @PostMapping("/requests/{id}/cancel")
    public ApiResponse<CertificateRequestDto> cancel(@PathVariable UUID id) {
        return ApiResponse.ok(requestService.cancel(id), "certificate.request_cancelled");
    }

    @Operation(summary = "Principal approves a certificate request")
    @PreAuthorize("hasAuthority('CERTIFICATE_APPROVE')")
    @PostMapping("/requests/{id}/approve")
    public ApiResponse<CertificateRequestDto> approveRequest(@PathVariable UUID id) {
        return ApiResponse.ok(requestService.approve(id), "certificate.approved");
    }

    @Operation(summary = "Principal rejects a certificate request")
    @PreAuthorize("hasAuthority('CERTIFICATE_APPROVE')")
    @PostMapping("/requests/{id}/reject")
    public ApiResponse<CertificateRequestDto> reject(
            @PathVariable UUID id,
            @Valid @RequestBody RejectCertificateRequest request) {
        return ApiResponse.ok(requestService.reject(id, request), "certificate.rejected");
    }

    @Operation(summary = "Certificate register")
    @PreAuthorize("hasAuthority('CERTIFICATE_READ')")
    @GetMapping
    public ApiResponse<PagedResponse<CertificateIssuedDto>> register(
            @RequestParam(required = false) CertificateType type,
            @RequestParam(required = false) CertificateStatus status,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(certificateService.register(type, status, query, page, size));
    }

    @Operation(summary = "Certificates issued to the current learner")
    @PreAuthorize("hasAuthority('CERTIFICATE_READ')")
    @GetMapping("/mine")
    public ApiResponse<List<CertificateIssuedDto>> mine() {
        return ApiResponse.ok(certificateService.mine());
    }

    @Operation(summary = "List issued certificates for a student")
    @PreAuthorize("hasAuthority('CERTIFICATE_READ')")
    @GetMapping("/student/{studentId}")
    public ApiResponse<List<CertificateIssuedDto>> listForStudent(@PathVariable UUID studentId) {
        return ApiResponse.ok(certificateService.listForStudent(studentId));
    }

    @Operation(summary = "Approve a draft certificate")
    @PreAuthorize("hasAuthority('CERTIFICATE_APPROVE')")
    @PostMapping("/{id}/approve")
    public ApiResponse<CertificateIssuedDto> approve(@PathVariable UUID id) {
        return ApiResponse.ok(certificateService.approve(id), "certificate.approved");
    }

    @Operation(summary = "Download a certificate PDF")
    @PreAuthorize("hasAuthority('CERTIFICATE_READ')")
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean reprint) {
        byte[] body = certificateService.download(id, reprint);
        String filename = "certificate-" + id + ".pdf";
        return ResponseEntity.ok()
                .headers(certificateService.downloadHeaders(filename))
                .body(body);
    }
}
