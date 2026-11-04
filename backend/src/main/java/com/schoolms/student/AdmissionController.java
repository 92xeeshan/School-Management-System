package com.schoolms.student;

import com.schoolms.academics.dto.ClassSubjectOptionDto;
import com.schoolms.common.api.ApiResponse;
import com.schoolms.common.enums.StudentDocumentType;
import com.schoolms.student.dto.AdmissionStep1Request;
import com.schoolms.student.dto.AdmissionStep2Request;
import com.schoolms.student.dto.AdmissionStep3Request;
import com.schoolms.student.dto.AdmissionStep4Request;
import com.schoolms.student.dto.AdmissionStep5Request;
import com.schoolms.student.dto.AdmissionStep6Request;
import com.schoolms.student.dto.AdmissionWizardDto;
import com.schoolms.student.dto.SiblingDto;
import com.schoolms.student.dto.StudentDocumentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Admission")
public class AdmissionController {

    private final AdmissionService admissionService;

    @Operation(summary = "Create a student from admission step 1")
    @PreAuthorize("hasAuthority('STUDENT_CREATE')")
    @PostMapping("/api/students/admission")
    public ApiResponse<AdmissionWizardDto> create(@Valid @RequestBody AdmissionStep1Request request) {
        return ApiResponse.ok(admissionService.create(request), "student.created");
    }

    @Operation(summary = "Load the admission wizard for a student")
    @PreAuthorize("hasAuthority('STUDENT_READ')")
    @GetMapping("/api/students/{id}/admission")
    public ApiResponse<AdmissionWizardDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(admissionService.get(id));
    }

    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    @PutMapping("/api/students/{id}/steps/1")
    public ApiResponse<AdmissionWizardDto> step1(@PathVariable UUID id,
                                                 @Valid @RequestBody AdmissionStep1Request request) {
        return ApiResponse.ok(admissionService.saveStep(id, 1, request));
    }

    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    @PutMapping("/api/students/{id}/steps/2")
    public ApiResponse<AdmissionWizardDto> step2(@PathVariable UUID id,
                                                 @Valid @RequestBody AdmissionStep2Request request) {
        return ApiResponse.ok(admissionService.saveStep(id, 2, request));
    }

    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    @PutMapping("/api/students/{id}/steps/3")
    public ApiResponse<AdmissionWizardDto> step3(@PathVariable UUID id,
                                                 @Valid @RequestBody AdmissionStep3Request request) {
        return ApiResponse.ok(admissionService.saveStep(id, 3, request));
    }

    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    @PutMapping("/api/students/{id}/steps/4")
    public ApiResponse<AdmissionWizardDto> step4(@PathVariable UUID id,
                                                 @Valid @RequestBody AdmissionStep4Request request) {
        return ApiResponse.ok(admissionService.saveStep(id, 4, request));
    }

    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    @PutMapping("/api/students/{id}/steps/5")
    public ApiResponse<AdmissionWizardDto> step5(@PathVariable UUID id,
                                                 @Valid @RequestBody AdmissionStep5Request request) {
        return ApiResponse.ok(admissionService.saveStep(id, 5, request));
    }

    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    @PutMapping("/api/students/{id}/steps/6")
    public ApiResponse<AdmissionWizardDto> step6(@PathVariable UUID id,
                                                 @Valid @RequestBody AdmissionStep6Request request) {
        return ApiResponse.ok(admissionService.saveStep(id, 6, request));
    }

    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    @PostMapping(value = "/api/students/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AdmissionWizardDto> photo(@PathVariable UUID id,
                                                 @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(admissionService.uploadPhoto(id, file));
    }

    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    @PostMapping(value = "/api/students/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StudentDocumentDto> uploadDocument(@PathVariable UUID id,
                                                          @RequestParam StudentDocumentType docType,
                                                          @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(admissionService.uploadDocument(id, docType, file));
    }

    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    @DeleteMapping("/api/students/{id}/documents/{docId}")
    public ApiResponse<Void> deleteDocument(@PathVariable UUID id, @PathVariable UUID docId) {
        admissionService.deleteDocument(id, docId);
        return ApiResponse.okMessage("student.document_deleted");
    }

    @PreAuthorize("hasAuthority('STUDENT_READ')")
    @GetMapping("/api/classes/{id}/subjects")
    public ApiResponse<List<ClassSubjectOptionDto>> classSubjects(@PathVariable UUID id,
                                                                  @RequestParam(required = false) UUID streamId) {
        return ApiResponse.ok(admissionService.classSubjects(id, streamId));
    }

    @PreAuthorize("hasAuthority('STUDENT_READ')")
    @GetMapping("/api/students/siblings")
    public ApiResponse<List<SiblingDto>> siblings(@RequestParam(required = false) String query) {
        return ApiResponse.ok(admissionService.searchSiblings(query));
    }

    @PreAuthorize("hasAuthority('CLASS_READ')")
    @GetMapping("/api/streams")
    public ApiResponse<List<com.schoolms.academics.dto.StreamDto>> streams() {
        return ApiResponse.ok(admissionService.listStreams());
    }

    @PreAuthorize("hasAuthority('CLASS_CREATE')")
    @PostMapping("/api/streams")
    public ApiResponse<com.schoolms.academics.dto.StreamDto> createStream(
            @Valid @RequestBody com.schoolms.academics.dto.StreamRequest request) {
        return ApiResponse.ok(admissionService.createStream(request));
    }

    @PreAuthorize("hasAuthority('CLASS_UPDATE')")
    @PutMapping("/api/streams/{id}")
    public ApiResponse<com.schoolms.academics.dto.StreamDto> updateStream(
            @PathVariable UUID id,
            @Valid @RequestBody com.schoolms.academics.dto.StreamRequest request) {
        return ApiResponse.ok(admissionService.updateStream(id, request));
    }

    @PreAuthorize("hasAuthority('CLASS_READ')")
    @GetMapping("/api/lookups/{kind}")
    public ApiResponse<List<com.schoolms.academics.dto.LookupDto>> lookups(
            @PathVariable com.schoolms.common.enums.LookupKind kind) {
        return ApiResponse.ok(admissionService.listLookups(kind));
    }

    @PreAuthorize("hasAuthority('CLASS_CREATE')")
    @PostMapping("/api/lookups/{kind}")
    public ApiResponse<com.schoolms.academics.dto.LookupDto> createLookup(
            @PathVariable com.schoolms.common.enums.LookupKind kind,
            @Valid @RequestBody com.schoolms.academics.dto.LookupRequest request) {
        return ApiResponse.ok(admissionService.createLookup(kind, request));
    }

    @PreAuthorize("hasAuthority('CLASS_UPDATE')")
    @PutMapping("/api/lookups/{id}")
    public ApiResponse<com.schoolms.academics.dto.LookupDto> updateLookup(
            @PathVariable UUID id,
            @Valid @RequestBody com.schoolms.academics.dto.LookupRequest request) {
        return ApiResponse.ok(admissionService.updateLookup(id, request));
    }
}
