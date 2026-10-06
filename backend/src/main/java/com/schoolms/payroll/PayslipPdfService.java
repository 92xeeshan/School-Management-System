package com.schoolms.payroll;

import com.schoolms.common.exception.BusinessException;
import com.schoolms.payroll.dto.SalaryLineItemDto;
import com.schoolms.school.School;
import com.schoolms.school.SchoolRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayslipPdfService {

    private final SchoolRepository schoolRepository;

    private JasperReport report;

    @PostConstruct
    void compileTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("reports/payslip.jrxml");
            try (InputStream in = resource.getInputStream()) {
                report = JasperCompileManager.compileReport(in);
            }
        } catch (Exception e) {
            log.error("Failed to pre-compile payslip Jasper template", e);
            throw new IllegalStateException("payslip template compile failed", e);
        }
    }

    public byte[] render(UUID schoolId, PayrollRun run, Payslip payslip) {
        School school = schoolRepository.findById(schoolId).orElse(null);
        Map<String, Object> params = new HashMap<>();
        params.put("schoolName", school == null ? "School" : school.getName());
        params.put("schoolAddress", school == null || school.getAddress() == null ? "" : school.getAddress());
        params.put("schoolPhone", school == null || school.getPhone() == null ? "" : school.getPhone());
        params.put("period", monthLabel(run.getMonth()) + " " + run.getYear());
        params.put("employeeNo", blank(payslip.getEmployeeNo()));
        params.put("staffName", blank(payslip.getStaffName()));
        params.put("designation", blank(payslip.getDesignation()));
        params.put("department", blank(payslip.getDepartment()));
        params.put("basic", money(payslip.getBasic()));
        params.put("hra", money(payslip.getHra()));
        params.put("allowances", formatLines(SalaryJson.fromJson(payslip.getAllowancesJson())));
        params.put("deductions", formatLines(SalaryJson.fromJson(payslip.getDeductionsJson())));
        params.put("gross", money(payslip.getGross()));
        params.put("totalDeductions", money(payslip.getTotalDeductions()));
        params.put("net", money(payslip.getNet()));
        try {
            JasperPrint print = JasperFillManager.fillReport(report, params, new JREmptyDataSource());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(print, out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to render payslip PDF", e);
            throw new BusinessException("payroll.export_failed");
        }
    }

    private static String formatLines(List<SalaryLineItemDto> items) {
        if (items == null || items.isEmpty()) {
            return "None";
        }
        return items.stream()
                .map(item -> item.name() + ": " + money(item.amount()))
                .collect(Collectors.joining("\n"));
    }

    private static String monthLabel(short month) {
        if (month < 1 || month > 12) {
            return String.valueOf(month);
        }
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private static String money(java.math.BigDecimal value) {
        return value == null ? "0.00" : value.toPlainString();
    }

    private static String blank(String value) {
        return value == null ? "" : value;
    }
}
