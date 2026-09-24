package com.schoolms.exam;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.exam.dto.MarksheetDto;
import com.schoolms.exam.dto.MarksheetSubjectDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class MarksheetPdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
    private static final Font SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 8);
    private static final Font MUTED = FontFactory.getFont(FontFactory.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);
    private static final Color HEADER_BG = new Color(226, 232, 240);
    private static final Color MALE_BG = new Color(191, 219, 254);
    private static final Color FEMALE_BG = new Color(251, 207, 232);
    private static final Color OTHER_BG = new Color(226, 232, 240);

    public byte[] render(List<MarksheetDto> cards) {
        if (cards == null || cards.isEmpty()) {
            throw new BusinessException("marksheet.no_students");
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 28, 28, 28, 28);
            PdfWriter.getInstance(document, out);
            document.open();
            for (int i = 0; i < cards.size(); i++) {
                if (i > 0) {
                    document.newPage();
                }
                writeCard(document, cards.get(i));
            }
            document.close();
            return out.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to render marksheet PDF", e);
            throw new BusinessException("marksheet.export_failed");
        }
    }

    private void writeCard(Document document, MarksheetDto card) throws DocumentException {
        document.add(header(card));
        document.add(heading("Candidate particulars"));
        document.add(meta(card));
        document.add(heading("Statement of marks"));
        document.add(subjects(card));
        document.add(totals(card));
        document.add(security(card));
        document.add(signatures());
    }

    private PdfPTable header(MarksheetDto card) {
        PdfPTable table = new PdfPTable(new float[] {5.2f, 1.3f});
        table.setWidthPercentage(100);
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(centered(blank(card.schoolName()), TITLE));
        left.addElement(centered(blank(card.affiliation()), MUTED));
        left.addElement(centered(blank(card.schoolAddress()), MUTED));
        String contact = join(card.schoolPhone(), card.schoolEmail());
        if (!contact.isBlank()) {
            left.addElement(centered(contact, MUTED));
        }
        left.addElement(centered("OFFICIAL MARKSHEET  |  " + blank(card.academicYearName())
                + "  |  " + blank(card.examTerm()), SUBTITLE));
        table.addCell(left);
        table.addCell(photoCell(card));
        return table;
    }

    private PdfPCell photoCell(MarksheetDto card) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setFixedHeight(82);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (card.photoUrl() != null && !card.photoUrl().isBlank()) {
            try {
                Image image = Image.getInstance(card.photoUrl());
                image.scaleToFit(62, 74);
                cell.addElement(image);
                return cell;
            } catch (Exception ignored) {
                // gender placeholder
            }
        }
        String gender = blank(card.gender());
        Color bg = "FEMALE".equals(gender) ? FEMALE_BG : ("MALE".equals(gender) ? MALE_BG : OTHER_BG);
        cell.setBackgroundColor(bg);
        Paragraph icon = new Paragraph(placeholderLabel(gender), TITLE);
        icon.setAlignment(Element.ALIGN_CENTER);
        Paragraph caption = new Paragraph(placeholderCaption(gender), MUTED);
        caption.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(icon);
        cell.addElement(caption);
        return cell;
    }

    private PdfPTable meta(MarksheetDto card) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        addMeta(table, "Name", blank(card.studentName()));
        addMeta(table, "Enrollment ID", blank(card.admissionNo()));
        addMeta(table, "Roll", card.rollNumber() == null ? "—" : String.valueOf(card.rollNumber()));
        addMeta(table, "Class / Section", blank(card.className()) + " - " + blank(card.sectionName()));
        addMeta(table, "Date of birth", card.dateOfBirth() == null ? "—" : DATE.format(card.dateOfBirth()));
        addMeta(table, "Gender", blank(card.gender()));
        addMeta(table, "Serial no.", blank(card.serialNo()));
        addMeta(table, "Issue date", card.issueDate() == null ? "—" : DATE.format(card.issueDate()));
        return table;
    }

    private PdfPTable subjects(MarksheetDto card) {
        PdfPTable table = new PdfPTable(new float[] {1.2f, 2.4f, 1.1f, 1.1f, 1.2f, 1.0f, 1.0f, 1.1f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        headerCell(table, "Subject code");
        headerCell(table, "Subject name");
        headerCell(table, "Max marks");
        headerCell(table, "Passing marks");
        headerCell(table, "Marks obtained");
        headerCell(table, "Grade");
        headerCell(table, "Grade point");
        headerCell(table, "Result");
        if (card.subjects() != null) {
            for (MarksheetSubjectDto row : card.subjects()) {
                bodyCell(table, blank(row.subjectCode()));
                bodyCell(table, blank(row.subjectName()));
                bodyCell(table, formatNumber(row.maxMarks()));
                bodyCell(table, formatNumber(row.passingMarks()));
                bodyCell(table, formatNumber(row.marksObtained()));
                bodyCell(table, blank(row.grade()));
                bodyCell(table, formatNumber(row.gradePoint()));
                bodyCell(table, blank(row.result()));
            }
        }
        return table;
    }

    private PdfPTable totals(MarksheetDto card) {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);
        addMetaWide(table, "Total marks", formatNumber(card.totalObtained()) + " / " + formatNumber(card.totalMax()));
        addMetaWide(table, "Percentage", formatNumber(card.percentage()) + "%");
        addMetaWide(table, "GPA", formatNumber(card.gpa()));
        addMetaWide(table, "Overall grade", blank(card.overallGrade()));
        addMetaWide(table, "Qualification", blank(card.result()));
        return table;
    }

    private PdfPTable security(MarksheetDto card) {
        PdfPTable table = new PdfPTable(new float[] {3.4f, 1.2f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.BOX);
        left.setPadding(6);
        left.addElement(new Paragraph("Security metadata", LABEL));
        left.addElement(new Paragraph("Marksheet serial: " + blank(card.serialNo()), BODY));
        left.addElement(new Paragraph("Registration / Enrollment ID: " + blank(card.admissionNo()), BODY));
        left.addElement(new Paragraph("Issue date: "
                + (card.issueDate() == null ? "—" : DATE.format(card.issueDate())), BODY));
        left.addElement(new Paragraph("This document is a formal examination credential.", MUTED));
        table.addCell(left);
        PdfPCell qr = new PdfPCell();
        qr.setBorder(Rectangle.BOX);
        qr.setFixedHeight(72);
        qr.setHorizontalAlignment(Element.ALIGN_CENTER);
        qr.setVerticalAlignment(Element.ALIGN_MIDDLE);
        qr.setBackgroundColor(HEADER_BG);
        Paragraph box = new Paragraph("QR", SUBTITLE);
        box.setAlignment(Element.ALIGN_CENTER);
        Paragraph hint = new Paragraph("Verification code", MUTED);
        hint.setAlignment(Element.ALIGN_CENTER);
        qr.addElement(box);
        qr.addElement(hint);
        table.addCell(qr);
        return table;
    }

    private PdfPTable signatures() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(28);
        signCell(table, "Controller of Examinations");
        signCell(table, "School seal");
        return table;
    }

    private static void signCell(PdfPTable table, String label) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(8);
        Paragraph line = new Paragraph("____________________", BODY);
        line.setAlignment(Element.ALIGN_CENTER);
        Paragraph caption = new Paragraph(label, MUTED);
        caption.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(line);
        cell.addElement(caption);
        table.addCell(cell);
    }

    private static Paragraph heading(String text) {
        Paragraph p = new Paragraph(text.toUpperCase(), LABEL);
        p.setSpacingBefore(10);
        p.setSpacingAfter(2);
        return p;
    }

    private static Paragraph centered(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    private static void addMeta(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, LABEL));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(3);
        table.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, BODY));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(3);
        table.addCell(valueCell);
    }

    private static void addMetaWide(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(4);
        cell.addElement(new Paragraph(label, MUTED));
        cell.addElement(new Paragraph(value, LABEL));
        table.addCell(cell);
    }

    private static void headerCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, LABEL));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private static void bodyCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BODY));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private static String formatNumber(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private static String join(String a, String b) {
        if (a == null || a.isBlank()) {
            return b == null ? "" : b;
        }
        if (b == null || b.isBlank()) {
            return a;
        }
        return a + "  ·  " + b;
    }

    private static String placeholderLabel(String gender) {
        if ("FEMALE".equals(gender)) {
            return "F";
        }
        if ("MALE".equals(gender)) {
            return "M";
        }
        return "?";
    }

    private static String placeholderCaption(String gender) {
        if ("FEMALE".equals(gender)) {
            return "Female photo";
        }
        if ("MALE".equals(gender)) {
            return "Male photo";
        }
        return "Photo";
    }

    private static String blank(String value) {
        return value == null ? "" : value;
    }
}
