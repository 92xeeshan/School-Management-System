package com.schoolms.event;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
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
import com.schoolms.event.dto.SchoolEventDto;
import com.schoolms.school.School;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class HolidayPdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font MUTED = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);
    private static final Color HEADER_BG = new Color(226, 232, 240);

    public byte[] render(School school, int year, List<SchoolEventDto> holidays) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();
            writeHeader(document, school, year);
            document.add(table(holidays));
            if (holidays.isEmpty()) {
                Paragraph empty = new Paragraph("No holidays recorded for " + year + ".", MUTED);
                empty.setSpacingBefore(12);
                document.add(empty);
            }
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to render holiday calendar PDF", e);
            throw new BusinessException("event.export_failed");
        }
    }

    private void writeHeader(Document document, School school, int year) throws DocumentException {
        Paragraph title = new Paragraph(blank(school.getName()), TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        if (school.getAddress() != null && !school.getAddress().isBlank()) {
            Paragraph address = new Paragraph(school.getAddress(), MUTED);
            address.setAlignment(Element.ALIGN_CENTER);
            document.add(address);
        }
        Paragraph subtitle = new Paragraph("Academic Holiday Calendar  " + year, SUBTITLE);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(16);
        document.add(subtitle);
    }

    private PdfPTable table(List<SchoolEventDto> holidays) {
        PdfPTable table = new PdfPTable(new float[] {2.2f, 2.2f, 4.6f, 3f});
        table.setWidthPercentage(100);
        header(table, "Start");
        header(table, "End");
        header(table, "Holiday");
        header(table, "Notes");
        for (SchoolEventDto holiday : holidays) {
            cell(table, format(holiday.startDate()));
            cell(table, format(holiday.endDate() == null ? holiday.startDate() : holiday.endDate()));
            cell(table, blank(holiday.title()));
            cell(table, blank(holiday.description()));
        }
        return table;
    }

    private void header(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(6);
        cell.setBorder(Rectangle.BOX);
        table.addCell(cell);
    }

    private void cell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BODY));
        cell.setPadding(6);
        table.addCell(cell);
    }

    private String format(LocalDate date) {
        return date == null ? "—" : DATE.format(date);
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
