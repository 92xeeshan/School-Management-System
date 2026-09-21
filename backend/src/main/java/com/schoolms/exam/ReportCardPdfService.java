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
import com.schoolms.exam.dto.ReportCardAttendanceDto;
import com.schoolms.exam.dto.ReportCardBehaviourDto;
import com.schoolms.exam.dto.ReportCardDto;
import com.schoolms.exam.dto.ReportCardSubjectDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class ReportCardPdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
    private static final Font SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 8);
    private static final Font MUTED = FontFactory.getFont(FontFactory.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);
    private static final Color HEADER_BG = new Color(226, 232, 240);

    public byte[] render(List<ReportCardDto> cards) {
        if (cards == null || cards.isEmpty()) {
            throw new BusinessException("report_card.no_students");
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
            log.error("Failed to render report card PDF", e);
            throw new BusinessException("report_card.export_failed");
        }
    }

    private void writeCard(Document document, ReportCardDto card) throws DocumentException {
        document.add(header(card));
        document.add(heading("Student particulars"));
        document.add(meta(card));
        document.add(heading("Attendance"));
        document.add(attendance(card.attendance()));
        document.add(heading("Scholastic areas"));
        document.add(subjects(card));
        document.add(totals(card));
        document.add(heading("Co-scholastic areas and remarks"));
        document.add(behaviour(card.behaviour()));
        document.add(commentBox("Class teacher remarks", card.teacherComment()));
        document.add(commentBox("Principal remarks", card.principalComment()));
        document.add(signatures(card));
    }

    private PdfPTable header(ReportCardDto card) {
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
        left.addElement(centered("HOLISTIC REPORT CARD  |  " + blank(card.academicYearName())
                + "  |  " + blank(card.examTerm()), SUBTITLE));
        table.addCell(left);
        table.addCell(photoCell(card));
        return table;
    }

    private PdfPCell photoCell(ReportCardDto card) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setFixedHeight(78);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (card.photoUrl() != null && !card.photoUrl().isBlank()) {
            try {
                Image image = Image.getInstance(card.photoUrl());
                image.scaleToFit(62, 74);
                cell.addElement(image);
                return cell;
            } catch (Exception ignored) {
                // fall through to initials
            }
        }
        String initial = blank(card.studentName()).isBlank() ? "?" : blank(card.studentName()).substring(0, 1);
        Paragraph p = new Paragraph(initial, TITLE);
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);
        return cell;
    }

    private PdfPTable meta(ReportCardDto card) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        addMeta(table, "Name", blank(card.studentName()));
        addMeta(table, "Admission No", blank(card.admissionNo()));
        addMeta(table, "Roll", card.rollNumber() == null ? "—" : String.valueOf(card.rollNumber()));
        addMeta(table, "Class / Section", blank(card.className()) + " - " + blank(card.sectionName()));
        addMeta(table, "Date of birth", card.dateOfBirth() == null ? "—" : DATE.format(card.dateOfBirth()));
        addMeta(table, "Guardian", blank(card.guardianName()).isBlank() ? "—" : card.guardianName());
        addMeta(table, "Class teacher", blank(card.classTeacherName()).isBlank() ? "—" : card.classTeacherName());
        addMeta(table, "Result", blank(card.result()) + " / " + blank(card.overallGrade()));
        return table;
    }

    private PdfPTable attendance(ReportCardAttendanceDto attendance) {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        headerCell(table, "Working days");
        headerCell(table, "Present");
        headerCell(table, "Absent");
        headerCell(table, "Late");
        headerCell(table, "Leave");
        headerCell(table, "%");
        if (attendance == null) {
            for (int i = 0; i < 6; i++) {
                bodyCell(table, "0");
            }
            return table;
        }
        bodyCell(table, String.valueOf(attendance.workingDays()));
        bodyCell(table, String.valueOf(attendance.daysPresent()));
        bodyCell(table, String.valueOf(attendance.daysAbsent()));
        bodyCell(table, String.valueOf(attendance.daysLate()));
        bodyCell(table, String.valueOf(attendance.daysLeave()));
        bodyCell(table, formatNumber(attendance.percent()));
        return table;
    }

    private PdfPTable subjects(ReportCardDto card) {
        PdfPTable table = new PdfPTable(new float[] {2.4f, 1.1f, 1.1f, 1.1f, 1.1f, 1.1f, 0.8f, 2.2f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        headerCell(table, "Subject");
        headerCell(table, "Formative");
        headerCell(table, "Theory");
        headerCell(table, "Practical");
        headerCell(table, "Total");
        headerCell(table, "Max");
        headerCell(table, "Grade");
        headerCell(table, "Remarks");
        if (card.subjects() != null) {
            for (ReportCardSubjectDto row : card.subjects()) {
                bodyCell(table, blank(row.subjectName()));
                bodyCell(table, marks(row.assignment(), row.maxAssignment()));
                bodyCell(table, marks(row.theory(), row.maxTheory()));
                bodyCell(table, marks(row.practical(), row.maxPractical()));
                bodyCell(table, formatNumber(row.total()));
                bodyCell(table, formatNumber(row.maxTotal()));
                bodyCell(table, blank(row.grade()));
                bodyCell(table, blank(row.remarks()));
            }
        }
        return table;
    }

    private PdfPTable totals(ReportCardDto card) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        addMeta(table, "Grand total", formatNumber(card.totalObtained()) + " / " + formatNumber(card.totalMax()));
        addMeta(table, "Percentage", formatNumber(card.percentage()) + "%");
        addMeta(table, "Overall grade", blank(card.overallGrade()));
        addMeta(table, "Result", blank(card.result()));
        return table;
    }

    private PdfPTable behaviour(ReportCardBehaviourDto behaviour) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        headerCell(table, "Conduct");
        headerCell(table, "Discipline");
        headerCell(table, "Punctuality");
        headerCell(table, "Co-curricular");
        if (behaviour == null) {
            bodyCell(table, "—");
            bodyCell(table, "—");
            bodyCell(table, "—");
            bodyCell(table, "—");
            return table;
        }
        bodyCell(table, label(behaviour.conduct()));
        bodyCell(table, label(behaviour.discipline()));
        bodyCell(table, label(behaviour.punctuality()));
        bodyCell(table, blank(behaviour.coCurricular()));
        return table;
    }

    private PdfPTable commentBox(String title, String text) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);
        PdfPCell head = new PdfPCell(new Phrase(title, LABEL));
        head.setBackgroundColor(HEADER_BG);
        head.setPadding(4);
        table.addCell(head);
        PdfPCell body = new PdfPCell(new Phrase(blank(text), BODY));
        body.setMinimumHeight(42);
        body.setPadding(6);
        table.addCell(body);
        return table;
    }

    private PdfPTable signatures(ReportCardDto card) {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(22);
        signCell(table, "Class teacher");
        signCell(table, "Parent / Guardian");
        signCell(table, "Principal / Official stamp");
        PdfPCell note = new PdfPCell(new Phrase(
                "Generated for " + blank(card.academicYearName()) + " · " + blank(card.examTerm()), MUTED));
        note.setColspan(3);
        note.setBorder(Rectangle.NO_BORDER);
        note.setPaddingTop(8);
        table.addCell(note);
        return table;
    }

    private static void signCell(PdfPTable table, String label) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(6);
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

    private static String marks(BigDecimal value, BigDecimal max) {
        if (max == null || max.compareTo(BigDecimal.ZERO) == 0) {
            return "—";
        }
        return formatNumber(value);
    }

    private static String formatNumber(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private static String label(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        return value.replace('_', ' ');
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

    private static String blank(String value) {
        return value == null ? "" : value;
    }
}
