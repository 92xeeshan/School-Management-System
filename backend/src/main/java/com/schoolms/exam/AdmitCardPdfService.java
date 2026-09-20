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
import com.schoolms.exam.dto.AdmitCardDto;
import com.schoolms.exam.dto.AdmitCardSlotDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class AdmitCardPdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font MUTED = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);

    public byte[] render(List<AdmitCardDto> cards) {
        if (cards == null || cards.isEmpty()) {
            throw new BusinessException("admit_card.no_students");
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
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
            log.error("Failed to render admit card PDF", e);
            throw new BusinessException("admit_card.export_failed");
        }
    }

    private void writeCard(Document document, AdmitCardDto card) throws DocumentException {
        PdfPTable border = new PdfPTable(1);
        border.setWidthPercentage(100);
        PdfPCell wrapper = new PdfPCell();
        wrapper.setBorderWidth(1);
        wrapper.setPadding(12);
        wrapper.addElement(header(card));
        wrapper.addElement(meta(card));
        wrapper.addElement(heading("Examination datesheet"));
        wrapper.addElement(datesheet(card));
        wrapper.addElement(heading("Candidate guidelines"));
        if (card.guidelines() != null) {
            for (String line : card.guidelines()) {
                wrapper.addElement(new Paragraph(line, BODY));
            }
        }
        wrapper.addElement(signatures());
        border.addCell(wrapper);
        document.add(border);
    }

    private PdfPTable header(AdmitCardDto card) {
        PdfPTable table = new PdfPTable(new float[] {5, 1.2f});
        table.setWidthPercentage(100);
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(centered(blank(card.schoolName()), TITLE));
        left.addElement(centered(blank(card.schoolAddress()), MUTED));
        left.addElement(centered(blank(card.schoolPhone()), MUTED));
        left.addElement(centered("ADMIT CARD / HALL TICKET", SUBTITLE));
        table.addCell(left);
        table.addCell(photoCell(card));
        return table;
    }

    private PdfPCell photoCell(AdmitCardDto card) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setFixedHeight(90);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (card.photoUrl() != null && !card.photoUrl().isBlank()) {
            try {
                Image image = Image.getInstance(card.photoUrl());
                image.scaleToFit(72, 86);
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

    private PdfPTable meta(AdmitCardDto card) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        addMeta(table, "Name", blank(card.studentName()));
        addMeta(table, "Admission No", blank(card.admissionNo()));
        addMeta(table, "Roll", card.rollNumber() == null ? "—" : String.valueOf(card.rollNumber()));
        addMeta(table, "Class / Section", blank(card.className()) + " - " + blank(card.sectionName()));
        addMeta(table, "Term", blank(card.examTerm()));
        addMeta(table, "Academic year", blank(card.academicYearName()));
        return table;
    }

    private PdfPTable datesheet(AdmitCardDto card) {
        PdfPTable table = new PdfPTable(new float[] {3, 2, 2, 2});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        headerCell(table, "Subject");
        headerCell(table, "Date");
        headerCell(table, "Time");
        headerCell(table, "Room");
        if (card.datesheet() != null) {
            for (AdmitCardSlotDto slot : card.datesheet()) {
                bodyCell(table, blank(slot.subjectName()));
                bodyCell(table, slot.examDate() == null ? "" : DATE.format(slot.examDate()));
                bodyCell(table, formatTime(slot.startTime()) + " - " + formatTime(slot.endTime()));
                bodyCell(table, slot.room() == null || slot.room().isBlank() ? "—" : slot.room());
            }
        }
        return table;
    }

    private Paragraph signatures() {
        Paragraph p = new Paragraph("Student signature                                        Controller of Examinations", BODY);
        p.setSpacingBefore(28);
        return p;
    }

    private static Paragraph heading(String text) {
        Paragraph p = new Paragraph(text, SUBTITLE);
        p.setSpacingBefore(12);
        p.setSpacingAfter(4);
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
        table.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, BODY));
        valueCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(valueCell);
    }

    private static void headerCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, LABEL));
        cell.setBackgroundColor(new Color(226, 232, 240));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private static void bodyCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BODY));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private static String formatTime(java.time.LocalTime time) {
        return time == null ? "" : TIME.format(time);
    }

    private static String blank(String value) {
        return value == null ? "" : value;
    }
}
