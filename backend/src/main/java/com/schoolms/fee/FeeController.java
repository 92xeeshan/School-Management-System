package com.schoolms.fee;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.fee.dto.FeeAssignmentDto;
import com.schoolms.fee.dto.FeeAssignmentRequest;
import com.schoolms.fee.dto.FeeCategoryDto;
import com.schoolms.fee.dto.FeeCategoryRequest;
import com.schoolms.fee.dto.FeePaymentDto;
import com.schoolms.fee.dto.FeeStructureDto;
import com.schoolms.fee.dto.FeeStructureRequest;
import com.schoolms.fee.dto.InstallmentDto;
import com.schoolms.fee.dto.PaymentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@Tag(name = "Fees")
@RequestMapping("/api/fees")
public class FeeController {

    private final FeeService feeService;

    // ---- Categories ------------------------------------------------------------
    @Operation(summary = "List fee categories")
    @PreAuthorize("hasAuthority('FEE_READ')")
    @GetMapping("/categories")
    public ApiResponse<List<FeeCategoryDto>> listCategories() {
        return ApiResponse.ok(feeService.listCategories());
    }

    @Operation(summary = "Create a fee category")
    @PreAuthorize("hasAuthority('FEE_STRUCTURE_MANAGE')")
    @PostMapping("/categories")
    public ApiResponse<FeeCategoryDto> createCategory(@Valid @RequestBody FeeCategoryRequest request) {
        return ApiResponse.ok(feeService.createCategory(request));
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
    public ApiResponse<FeeStructureDto> createStructure(@Valid @RequestBody FeeStructureRequest request) {
        return ApiResponse.ok(feeService.createStructure(request));
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
