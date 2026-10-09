package com.schoolms.fee;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.fee.dto.AdhocFeeLevyDto;
import com.schoolms.fee.dto.AdhocFeeLevyRequest;
import com.schoolms.fee.dto.CloneFeeStructureRequest;
import com.schoolms.fee.dto.CloneFeeStructureResult;
import com.schoolms.fee.dto.FeeAssignmentDto;
import com.schoolms.fee.dto.FeeAssignmentRequest;
import com.schoolms.fee.dto.FeeCategoryDto;
import com.schoolms.fee.dto.FeeCategoryRequest;
import com.schoolms.fee.dto.FeePaymentDto;
import com.schoolms.fee.dto.FeeStructureAuditDto;
import com.schoolms.fee.dto.FeeStructureDto;
import com.schoolms.fee.dto.FeeStructureRequest;
import com.schoolms.fee.dto.FeeStructureUpdateRequest;
import com.schoolms.fee.dto.InstallmentDto;
import com.schoolms.fee.dto.PaymentRequest;
import com.schoolms.fee.dto.SiblingDiscountRuleDto;
import com.schoolms.fee.dto.SiblingDiscountRuleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@Tag(name = "Fees")
@RequestMapping("/api/fees")
public class FeeController {

    private final FeeService feeService;

    // ---- Categories ------------------------------------------------------------
    @Operation(summary = "List fee heads")
    @PreAuthorize("hasAuthority('FEE_READ')")
    @GetMapping("/categories")
    public ApiResponse<List<FeeCategoryDto>> listCategories() {
        return ApiResponse.ok(feeService.listCategories());
    }

    @Operation(summary = "Create a fee head")
    @PreAuthorize("hasAuthority('FEE_STRUCTURE_MANAGE')")
    @PostMapping("/categories")
    public ApiResponse<FeeCategoryDto> createCategory(@Valid @RequestBody FeeCategoryRequest request) {
        return ApiResponse.ok(feeService.createCategory(request), "fee.category_created");
    }

    @Operation(summary = "Update a fee head")
    @PreAuthorize("hasAuthority('FEE_STRUCTURE_MANAGE')")
    @PutMapping("/categories/{id}")
    public ApiResponse<FeeCategoryDto> updateCategory(
            @PathVariable UUID id, @Valid @RequestBody FeeCategoryRequest request) {
        return ApiResponse.ok(feeService.updateCategory(id, request), "fee.category_updated");
    }

    @Operation(summary = "Deactivate a fee head")
    @PreAuthorize("hasAuthority('FEE_STRUCTURE_MANAGE')")
    @PostMapping("/categories/{id}/deactivate")
    public ApiResponse<FeeCategoryDto> deactivateCategory(@PathVariable UUID id) {
        return ApiResponse.ok(feeService.deactivateCategory(id), "fee.category_deactivated");
    }

    // ---- Structures -----------------------------------------------------------
    @Operation(summary = "List fee structures for an academic year")
    @PreAuthorize("hasAuthority('FEE_READ')")
    @GetMapping("/structures")
    public ApiResponse<List<FeeStructureDto>> listStructures(@RequestParam UUID academicYearId) {
        return ApiResponse.ok(feeService.listStructures(academicYearId));
    }

    @Operation(summary = "Create a fee structure")
    @PreAuthorize("hasAuthority('FEE_STRUCTURE_MANAGE')")
    @PostMapping("/structures")
    public ApiResponse<List<FeeStructureDto>> createStructure(@Valid @RequestBody FeeStructureRequest request) {
        return ApiResponse.ok(feeService.createStructure(request), "fee.structure_created");
    }

    @Operation(summary = "Update a fee structure")
    @PreAuthorize("hasAuthority('FEE_STRUCTURE_MANAGE')")
    @PutMapping("/structures/{id}")
    public ApiResponse<FeeStructureDto> updateStructure(
            @PathVariable UUID id, @Valid @RequestBody FeeStructureUpdateRequest request) {
        return ApiResponse.ok(feeService.updateStructure(id, request), "fee.structure_updated");
    }

    @Operation(summary = "Clone fee structures from a previous academic year")
    @PreAuthorize("hasAuthority('FEE_STRUCTURE_MANAGE')")
    @PostMapping("/structures/clone")
    public ApiResponse<CloneFeeStructureResult> cloneStructures(@Valid @RequestBody CloneFeeStructureRequest request) {
        return ApiResponse.ok(feeService.cloneStructures(request), "fee.structure_cloned");
    }

    @Operation(summary = "Fee structure change audit")
    @PreAuthorize("hasAuthority('FEE_STRUCTURE_MANAGE')")
    @GetMapping("/structures/audit")
    public ApiResponse<List<FeeStructureAuditDto>> listAudit(@RequestParam(required = false) UUID academicYearId) {
        return ApiResponse.ok(feeService.listAudit(academicYearId));
    }

    // ---- Sibling discounts ----------------------------------------------------
    @Operation(summary = "List sibling discount rules")
    @PreAuthorize("hasAuthority('FEE_READ')")
    @GetMapping("/discounts/sibling")
    public ApiResponse<List<SiblingDiscountRuleDto>> listSiblingRules(
            @RequestParam(required = false) UUID academicYearId) {
        return ApiResponse.ok(feeService.listSiblingRules(academicYearId));
    }

    @Operation(summary = "Create a sibling discount rule")
    @PreAuthorize("hasAuthority('FEE_STRUCTURE_MANAGE')")
    @PostMapping("/discounts/sibling")
    public ApiResponse<SiblingDiscountRuleDto> createSiblingRule(
            @Valid @RequestBody SiblingDiscountRuleRequest request) {
        return ApiResponse.ok(feeService.createSiblingRule(request), "fee.discount_created");
    }

    @Operation(summary = "Update a sibling discount rule")
    @PreAuthorize("hasAuthority('FEE_STRUCTURE_MANAGE')")
    @PutMapping("/discounts/sibling/{id}")
    public ApiResponse<SiblingDiscountRuleDto> updateSiblingRule(
            @PathVariable UUID id, @Valid @RequestBody SiblingDiscountRuleRequest request) {
        return ApiResponse.ok(feeService.updateSiblingRule(id, request), "fee.discount_updated");
    }

    // ---- Ad-hoc levies --------------------------------------------------------
    @Operation(summary = "List ad-hoc fee levies")
    @PreAuthorize("hasAuthority('FEE_READ')")
    @GetMapping("/adhoc")
    public ApiResponse<List<AdhocFeeLevyDto>> listLevies(@RequestParam(required = false) UUID academicYearId) {
        return ApiResponse.ok(feeService.listLevies(academicYearId));
    }

    @Operation(summary = "Levy an ad-hoc fee")
    @PreAuthorize("hasAuthority('FEE_STRUCTURE_MANAGE')")
    @PostMapping("/adhoc")
    public ApiResponse<AdhocFeeLevyDto> createLevy(@Valid @RequestBody AdhocFeeLevyRequest request) {
        return ApiResponse.ok(feeService.createLevy(request), "fee.adhoc_created");
    }

    // ---- Assignments -----------------------------------------------------------
    @Operation(summary = "Assign a fee structure to a student and generate installments")
    @PreAuthorize("hasAuthority('FEE_STRUCTURE_MANAGE')")
    @PostMapping("/assignments")
    public ApiResponse<FeeAssignmentDto> assign(@Valid @RequestBody FeeAssignmentRequest request) {
        return ApiResponse.ok(feeService.assign(request));
    }

    @Operation(summary = "List a student's fee assignments")
    @PreAuthorize("hasAnyAuthority('FEE_READ','STUDENT_READ')")
    @GetMapping("/students/{studentId}/assignments")
    public ApiResponse<List<FeeAssignmentDto>> listAssignments(@PathVariable UUID studentId) {
        return ApiResponse.ok(feeService.listAssignments(studentId));
    }

    @Operation(summary = "List installments for a fee assignment")
    @PreAuthorize("hasAnyAuthority('FEE_READ','STUDENT_READ')")
    @GetMapping("/assignments/{assignmentId}/installments")
    public ApiResponse<List<InstallmentDto>> listInstallments(@PathVariable UUID assignmentId) {
        return ApiResponse.ok(feeService.listInstallments(assignmentId));
    }

    // ---- Payments -------------------------------------------------------------
    @Operation(summary = "Record a fee payment and generate a PDF receipt")
    @PreAuthorize("hasAuthority('FEE_PAYMENT_RECORD')")
    @PostMapping("/payments")
    public ApiResponse<FeePaymentDto> recordPayment(@Valid @RequestBody PaymentRequest request) {
        return ApiResponse.ok(feeService.recordPayment(request));
    }

    @Operation(summary = "List a student's fee payments")
    @PreAuthorize("hasAnyAuthority('FEE_READ','FEE_RECEIPT_VIEW','STUDENT_READ')")
    @GetMapping("/students/{studentId}/payments")
    public ApiResponse<List<FeePaymentDto>> listPayments(@PathVariable UUID studentId) {
        return ApiResponse.ok(feeService.listPayments(studentId));
    }

    @Operation(summary = "Get a payment by id")
    @PreAuthorize("hasAnyAuthority('FEE_READ','FEE_RECEIPT_VIEW','STUDENT_READ')")
    @GetMapping("/payments/{id}")
    public ApiResponse<FeePaymentDto> getPayment(@PathVariable UUID id) {
        return ApiResponse.ok(feeService.getPayment(id));
    }
}
