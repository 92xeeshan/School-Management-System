package com.schoolms.academics;

import com.schoolms.academics.dto.AcademicTermDto;
import com.schoolms.academics.dto.AcademicTermRequest;
import com.schoolms.academics.dto.AcademicYearDetailDto;
import com.schoolms.academics.dto.AcademicYearDto;
import com.schoolms.academics.dto.AcademicYearRequest;
import com.schoolms.academics.dto.PromotionBatchDto;
import com.schoolms.academics.dto.PromotionPreviewDto;
import com.schoolms.academics.dto.PromotionPreviewRequest;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Academic Terms")
public class AcademicSessionController {

    private final AcademicSessionService sessionService;

    @Operation(summary = "List academic years with terms")
    @PreAuthorize("hasAnyAuthority('TERM_READ', 'CLASS_READ')")
    @GetMapping("/api/academic-sessions")
    public ApiResponse<List<AcademicYearDetailDto>> listSessions() {
        return ApiResponse.ok(sessionService.listSessions());
    }

    @Operation(summary = "Get an academic year with its terms")
    @PreAuthorize("hasAnyAuthority('TERM_READ', 'CLASS_READ')")
    @GetMapping("/api/academic-years/{id}")
    public ApiResponse<AcademicYearDetailDto> getYear(@PathVariable UUID id) {
        return ApiResponse.ok(sessionService.getYear(id));
    }

    @Operation(summary = "Update an academic year")
    @PreAuthorize("hasAnyAuthority('TERM_MANAGE', 'CLASS_CREATE')")
    @PutMapping("/api/academic-years/{id}")
    public ApiResponse<AcademicYearDto> updateYear(@PathVariable UUID id,
                                                   @Valid @RequestBody AcademicYearRequest request) {
        return ApiResponse.ok(sessionService.updateYear(id, request), "academic_year.updated");
    }

    @Operation(summary = "Mark an academic year as current")
    @PreAuthorize("hasAnyAuthority('TERM_MANAGE', 'CLASS_CREATE')")
    @PostMapping("/api/academic-years/{id}/activate")
    public ApiResponse<AcademicYearDto> activateYear(@PathVariable UUID id) {
        return ApiResponse.ok(sessionService.activateYear(id), "academic_year.activated");
    }

    @Operation(summary = "Clear the current flag on an academic year")
    @PreAuthorize("hasAnyAuthority('TERM_MANAGE', 'CLASS_CREATE')")
    @PostMapping("/api/academic-years/{id}/deactivate")
    public ApiResponse<AcademicYearDto> deactivateYear(@PathVariable UUID id) {
        return ApiResponse.ok(sessionService.deactivateYear(id), "academic_year.deactivated");
    }

    @Operation(summary = "Create a term in an academic year")
    @PreAuthorize("hasAnyAuthority('TERM_MANAGE', 'CLASS_CREATE')")
    @PostMapping("/api/academic-years/{id}/terms")
    public ApiResponse<AcademicTermDto> createTerm(@PathVariable UUID id,
                                                   @Valid @RequestBody AcademicTermRequest request) {
        return ApiResponse.ok(sessionService.createTerm(id, request), "academic_term.created");
    }

    @Operation(summary = "Update a term")
    @PreAuthorize("hasAnyAuthority('TERM_MANAGE', 'CLASS_CREATE')")
    @PutMapping("/api/academic-terms/{id}")
    public ApiResponse<AcademicTermDto> updateTerm(@PathVariable UUID id,
                                                   @Valid @RequestBody AcademicTermRequest request) {
        return ApiResponse.ok(sessionService.updateTerm(id, request), "academic_term.updated");
    }

    @Operation(summary = "Delete a term")
    @PreAuthorize("hasAnyAuthority('TERM_MANAGE', 'CLASS_CREATE')")
    @DeleteMapping("/api/academic-terms/{id}")
    public ApiResponse<Void> deleteTerm(@PathVariable UUID id) {
        sessionService.deleteTerm(id);
        return ApiResponse.okMessage("academic_term.deleted");
    }

    @Operation(summary = "Preview a batch promotion")
    @PreAuthorize("hasAnyAuthority('TERM_MANAGE', 'CLASS_CREATE')")
    @PostMapping("/api/promotions/preview")
    public ApiResponse<PromotionPreviewDto> preview(@Valid @RequestBody PromotionPreviewRequest request) {
        return ApiResponse.ok(sessionService.previewPromotion(request));
    }

    @Operation(summary = "Execute a batch promotion")
    @PreAuthorize("hasAnyAuthority('TERM_MANAGE', 'CLASS_CREATE')")
    @PostMapping("/api/promotions")
    public ApiResponse<PromotionBatchDto> execute(@Valid @RequestBody PromotionPreviewRequest request) {
        return ApiResponse.ok(sessionService.executePromotion(request), "promotion.completed");
    }

    @Operation(summary = "List promotion history")
    @PreAuthorize("hasAnyAuthority('TERM_READ', 'CLASS_READ')")
    @GetMapping("/api/promotions")
    public ApiResponse<List<PromotionBatchDto>> listPromotions() {
        return ApiResponse.ok(sessionService.listPromotions());
    }

    @Operation(summary = "Get a promotion batch with students")
    @PreAuthorize("hasAnyAuthority('TERM_READ', 'CLASS_READ')")
    @GetMapping("/api/promotions/{id}")
    public ApiResponse<PromotionBatchDto> getPromotion(@PathVariable UUID id) {
        return ApiResponse.ok(sessionService.getPromotion(id));
    }
}
