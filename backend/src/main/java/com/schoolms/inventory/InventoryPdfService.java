package com.schoolms.inventory;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.inventory.dto.InventoryItemDto;
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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryPdfService {

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 8);
    private static final Color HEADER_BG = new Color(226, 232, 240);
    private static final Color LOW_STOCK = new Color(254, 226, 226);

    private final SchoolRepository schoolRepository;

    private JasperReport report;

    @PostConstruct
    void compileTemplate() {
        System.setProperty("java.awt.headless", "true");
        try {
            ClassPathResource resource = new ClassPathResource("reports/inventory_report.jrxml");
            try (InputStream in = resource.getInputStream()) {
                report = JasperCompileManager.compileReport(in);
            }
        } catch (Exception e) {
            log.warn("Could not pre-compile inventory Jasper template, using OpenPDF fallback: {}", e.toString());
            report = null;
        }
    }

    public byte[] render(UUID schoolId, List<InventoryItemDto> items) {
        School school = schoolRepository.findById(schoolId).orElse(null);
        Map<String, Object> params = new HashMap<>();
        params.put("schoolName", school == null ? "School" : school.getName());
        params.put("schoolAddress", school == null || school.getAddress() == null ? "" : school.getAddress());
        params.put("generatedOn", LocalDate.now().toString());
        params.put("itemCount", String.valueOf(items.size()));
        params.put("rows", formatRows(items));
        if (report == null) {
            return renderFallback(params, items);
        }
        try {
            JasperPrint print = JasperFillManager.fillReport(report, params, new JREmptyDataSource());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(print, out);
            return out.toByteArray();
        } catch (Throwable e) {
            log.warn("Jasper inventory render failed, using OpenPDF fallback: {}", e.toString());
            return renderFallback(params, items);
        }
    }

    private byte[] renderFallback(Map<String, Object> params, List<InventoryItemDto> items) {
        try {
            return renderOpenPdf(params, items);
        } catch (Exception fallback) {
            log.error("Failed to render inventory PDF", fallback);
            throw new BusinessException("inventory.export_failed");
        }
    }

    private byte[] renderOpenPdf(Map<String, Object> params, List<InventoryItemDto> items) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
        PdfWriter.getInstance(document, out);
        document.open();

        Paragraph schoolName = new Paragraph(str(params, "schoolName"), TITLE);
        schoolName.setAlignment(Element.ALIGN_CENTER);
        document.add(schoolName);
        Paragraph address = new Paragraph(str(params, "schoolAddress"), BODY);
        address.setAlignment(Element.ALIGN_CENTER);
        document.add(address);
        Paragraph title = new Paragraph("Inventory Report", SUBTITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(8);
        title.setSpacingAfter(10);
        document.add(title);

        PdfPTable table = new PdfPTable(new float[] {3, 2, 1.6f, 1, 1, 1, 1.6f, 1.6f, 1.4f});
        table.setWidthPercentage(100);
        for (String header : List.of("Item", "Category", "SKU", "Qty", "Reorder", "Unit", "Location", "Condition", "Vendor")) {
            PdfPCell cell = new PdfPCell(new Phrase(header, LABEL));
            cell.setBackgroundColor(HEADER_BG);
            cell.setPadding(5);
            table.addCell(cell);
        }
        for (InventoryItemDto item : items) {
            Color bg = item.lowStock() ? LOW_STOCK : Color.WHITE;
            addCell(table, item.name(), bg);
            addCell(table, blank(item.categoryName()), bg);
            addCell(table, blank(item.sku()), bg);
            addCell(table, String.valueOf(item.quantity()), bg);
            addCell(table, String.valueOf(item.reorderThreshold()), bg);
            addCell(table, blank(item.unit()), bg);
            addCell(table, blank(item.location()), bg);
            addCell(table, item.condition() == null ? "" : item.condition().name(), bg);
            addCell(table, blank(item.vendor()), bg);
        }
        document.add(table);
        Paragraph footer = new Paragraph(
                "Generated on " + str(params, "generatedOn") + "  |  " + items.size() + " items", BODY);
        footer.setSpacingBefore(10);
        document.add(footer);
        document.close();
        return out.toByteArray();
    }

    private static void addCell(PdfPTable table, String value, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(value, BODY));
        cell.setBackgroundColor(bg);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private static String formatRows(List<InventoryItemDto> items) {
        if (items == null || items.isEmpty()) {
            return "No inventory items.";
        }
        return items.stream()
                .map(item -> item.name()
                        + " | " + blank(item.categoryName())
                        + " | SKU " + blank(item.sku())
                        + " | qty " + item.quantity()
                        + (item.lowStock() ? " (LOW)" : "")
                        + " | " + blank(item.location())
                        + " | " + (item.condition() == null ? "" : item.condition().name()))
                .collect(Collectors.joining("\n"));
    }

    private static String str(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static String blank(String value) {
        return value == null ? "" : value;
    }
}
