package com.schoolms.payroll;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
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

import java.awt.Color;
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

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
    private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font MUTED = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
    private static final Color HEADER_BG = new Color(226, 232, 240);

    private final SchoolRepository schoolRepository;

    private JasperReport report;

    @PostConstruct
    void compileTemplate() {
        System.setProperty("java.awt.headless", "true");
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
        } catch (Throwable e) {
            log.warn("Jasper payslip render failed, using OpenPDF fallback: {}", e.toString());
            try {
                return renderOpenPdf(params);
            } catch (Exception fallback) {
                log.error("Failed to render payslip PDF", fallback);
                throw new BusinessException("payroll.export_failed");
            }
        }
    }

    private byte[] renderOpenPdf(Map<String, Object> params) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, out);
        document.open();

        PdfPTable border = new PdfPTable(1);
        border.setWidthPercentage(100);
        PdfPCell wrapper = new PdfPCell();
        wrapper.setBorderWidth(1);
        wrapper.setPadding(16);
        wrapper.setBackgroundColor(Color.WHITE);

        wrapper.addElement(centered(str(params, "schoolName"), TITLE));
        wrapper.addElement(centered(str(params, "schoolAddress"), MUTED));
        wrapper.addElement(centered(str(params, "schoolPhone"), MUTED));
        Paragraph title = centered("Salary Payslip", SUBTITLE);
        title.setSpacingBefore(8);
        title.setSpacingAfter(12);
        wrapper.addElement(title);

        PdfPTable meta = new PdfPTable(new float[] {2, 5});
        meta.setWidthPercentage(100);
        addRow(meta, "Period", str(params, "period"));
        addRow(meta, "Employee No", str(params, "employeeNo"));
        addRow(meta, "Name", str(params, "staffName"));
        addRow(meta, "Designation", str(params, "designation"));
        addRow(meta, "Department", str(params, "department"));
        wrapper.addElement(meta);

        wrapper.addElement(section("Earnings"));
        PdfPTable earnings = new PdfPTable(new float[] {2, 5});
        earnings.setWidthPercentage(100);
        addRow(earnings, "Basic", str(params, "basic"));
        addRow(earnings, "HRA", str(params, "hra"));
        addRow(earnings, "Allowances", str(params, "allowances"));
        wrapper.addElement(earnings);

        wrapper.addElement(section("Deductions"));
        PdfPTable deductions = new PdfPTable(new float[] {2, 5});
        deductions.setWidthPercentage(100);
        addRow(deductions, "Items", str(params, "deductions"));
        addRow(deductions, "Total deductions", str(params, "totalDeductions"));
        wrapper.addElement(deductions);

        wrapper.addElement(section("Net pay"));
        PdfPTable totals = new PdfPTable(new float[] {2, 5});
        totals.setWidthPercentage(100);
        addRow(totals, "Gross", str(params, "gross"));
        addRow(totals, "Net pay", str(params, "net"));
        wrapper.addElement(totals);

        border.addCell(wrapper);
        document.add(border);
        document.close();
        return out.toByteArray();
    }

    private static Paragraph section(String text) {
        Paragraph p = new Paragraph(text, SUBTITLE);
        p.setSpacingBefore(14);
        p.setSpacingAfter(6);
        return p;
    }

    private static Paragraph centered(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    private static void addRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, LABEL));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setBackgroundColor(HEADER_BG);
        labelCell.setPadding(6);
        table.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, BODY));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(6);
        table.addCell(valueCell);
    }

    private static String str(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value == null ? "" : String.valueOf(value);
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
