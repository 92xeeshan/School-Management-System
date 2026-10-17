package com.schoolms.student;

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
import com.schoolms.student.dto.StudentDto;
import com.schoolms.student.dto.StudentListItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Renders the filtered student directory to a PDF table using OpenPDF. Kept
 * dependency-light so exporting never blocks on an external template engine.
 */
@Slf4j
@Service
public class StudentDirectoryPdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
    private static final Font SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
    private static final Font HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 8);
    private static final Color HEADER_BG = new Color(226, 232, 240);

    public byte[] render(String schoolName, List<StudentListItemDto> students) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
            PdfWriter.getInstance(document, out);
            document.open();
            Paragraph title = new Paragraph(schoolName == null || schoolName.isBlank()
                    ? "Student Directory" : schoolName, TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            Paragraph subtitle = new Paragraph(
                    "Student Directory  |  " + students.size() + " record(s)  |  Generated "
                            + DATE.format(LocalDate.now()), SUBTITLE);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(10);
            document.add(subtitle);
            document.add(table(students));
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to render student directory PDF", e);
            throw new BusinessException("student.export_failed");
        }
    }

    private PdfPTable table(List<StudentListItemDto> students) {
        PdfPTable table = new PdfPTable(new float[] {1.4f, 2.0f, 2.0f, 0.9f, 1.2f, 1.0f, 0.9f, 1.2f, 1.4f});
        table.setWidthPercentage(100);
        for (String heading : new String[] {"Admission No", "First Name", "Last Name", "Gender",
                "Class", "Section", "Roll", "Status", "Admission Date"}) {
            PdfPCell cell = new PdfPCell(new Phrase(heading, HEADER));
            cell.setBackgroundColor(HEADER_BG);
            cell.setPadding(5);
            table.addCell(cell);
        }
        for (StudentListItemDto item : students) {
            StudentDto s = item.student();
            cell(table, s.admissionNo());
            cell(table, s.firstName());
            cell(table, s.lastName());
            cell(table, s.gender() == null ? "" : s.gender().name());
            cell(table, item.className());
            cell(table, item.sectionName());
            cell(table, item.rollNumber() == null ? "" : String.valueOf(item.rollNumber()));
            cell(table, s.status() == null ? "" : s.status().name());
            cell(table, s.admissionDate() == null ? "" : DATE.format(s.admissionDate()));
        }
        return table;
    }

    private static void cell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, BODY));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(new Color(203, 213, 225));
        cell.setPadding(4);
        table.addCell(cell);
    }
}
