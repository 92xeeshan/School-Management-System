package com.schoolms.payroll;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.payroll.dto.PayrollRunDto;
import com.schoolms.payroll.dto.PayrollRunRequest;
import com.schoolms.payroll.dto.PayslipDto;
import com.schoolms.payroll.dto.SalaryStructureDto;
import com.schoolms.payroll.dto.SalaryStructureRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Payroll")
@RequestMapping("/api/payroll")
public class PayrollController {

    private final PayrollService payrollService;

    @Operation(summary = "List salary structures")
    @PreAuthorize("hasAuthority('PAYROLL_READ')")
    @GetMapping("/salaries")
    public ApiResponse<List<SalaryStructureDto>> listSalaries() {
        return ApiResponse.ok(payrollService.listSalaryStructures());
    }

    @Operation(summary = "Create a salary structure")
    @PreAuthorize("hasAuthority('PAYROLL_MANAGE')")
    @PostMapping("/salaries")
    public ApiResponse<SalaryStructureDto> createSalary(@Valid @RequestBody SalaryStructureRequest request) {
        return ApiResponse.ok(payrollService.upsertSalaryStructure(request), "payroll.salary_saved");
    }

    @Operation(summary = "List payroll runs")
    @PreAuthorize("hasAuthority('PAYROLL_READ')")
    @GetMapping("/runs")
    public ApiResponse<List<PayrollRunDto>> listRuns() {
        return ApiResponse.ok(payrollService.listRuns());
    }

    @Operation(summary = "Create a payroll run")
    @PreAuthorize("hasAuthority('PAYROLL_MANAGE')")
    @PostMapping("/runs")
    public ApiResponse<PayrollRunDto> createRun(@Valid @RequestBody PayrollRunRequest request) {
        return ApiResponse.ok(payrollService.createRun(request), "payroll.run_created");
    }

    @Operation(summary = "Process a draft payroll run")
    @PreAuthorize("hasAuthority('PAYROLL_MANAGE')")
    @PostMapping("/run/{id}/process")
    public ApiResponse<PayrollRunDto> process(@PathVariable UUID id) {
        return ApiResponse.ok(payrollService.process(id), "payroll.processed");
    }

    @Operation(summary = "Publish a processed payroll run")
    @PreAuthorize("hasAuthority('PAYROLL_PUBLISH')")
    @PostMapping("/run/{id}/publish")
    public ApiResponse<PayrollRunDto> publish(@PathVariable UUID id) {
        return ApiResponse.ok(payrollService.publish(id), "payroll.published");
    }

    @Operation(summary = "Mark a published payroll run as paid")
    @PreAuthorize("hasAuthority('PAYROLL_MANAGE')")
    @PostMapping("/run/{id}/paid")
    public ApiResponse<PayrollRunDto> markPaid(@PathVariable UUID id) {
        return ApiResponse.ok(payrollService.markPaid(id), "payroll.paid");
    }

    @Operation(summary = "List payslips for a payroll run")
    @PreAuthorize("hasAuthority('PAYROLL_READ')")
    @GetMapping("/run/{id}/payslips")
    public ApiResponse<List<PayslipDto>> listPayslips(@PathVariable UUID id) {
        return ApiResponse.ok(payrollService.listPayslips(id));
    }

    @Operation(summary = "List published payslips for the logged-in staff member")
    @PreAuthorize("hasAuthority('PAYSLIP_READ')")
    @GetMapping("/my-payslips")
    public ApiResponse<List<PayslipDto>> myPayslips() {
        return ApiResponse.ok(payrollService.myPayslips());
    }

    @Operation(summary = "Download a published payslip PDF")
    @PreAuthorize("hasAuthority('PAYSLIP_READ')")
    @GetMapping("/my-payslips/{id}/download")
    public ResponseEntity<byte[]> downloadMyPayslip(@PathVariable UUID id) {
        byte[] pdf = payrollService.downloadMyPayslip(id);
        return ResponseEntity.ok()
                .headers(payrollService.downloadHeaders(payrollService.monthOf(id), payrollService.yearOf(id)))
                .body(pdf);
    }
}
