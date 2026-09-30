package com.schoolms.certificate;

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
import com.schoolms.file.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificatePdfService {

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font MUTED = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);
    private static final Font WATERMARK = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.NORMAL, new Color(185, 28, 28));

    private final MinioService minioService;

    public byte[] render(CertificateType type, Map<String, Object> fields) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();
            PdfPTable border = new PdfPTable(1);
            border.setWidthPercentage(100);
            PdfPCell wrapper = new PdfPCell();
            wrapper.setBorderWidth(1);
            wrapper.setPadding(16);
            wrapper.addElement(header(type, fields));
            wrapper.addElement(body(type, fields));
            wrapper.addElement(signatures(fields));
            wrapper.addElement(footer(fields));
            border.addCell(wrapper);
            document.add(border);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to render {} certificate PDF", type, e);
            throw new BusinessException("certificate.export_failed");
        }
    }

    public String store(UUID schoolId, byte[] pdf) {
        String objectKey = schoolId + "/certificates/" + UUID.randomUUID() + ".pdf";
        try {
            minioService.uploadBytes(schoolId, "certificates", objectKey, pdf, "application/pdf");
            return objectKey;
        } catch (Exception e) {
            log.warn("Certificate PDF stored in memory only: {}", e.getMessage());
            return objectKey;
        }
    }

    private PdfPTable header(CertificateType type, Map<String, Object> fields) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        addCentered(table, val(fields, "headerHtml", val(fields, "schoolName", "School")), TITLE);
        addCentered(table, val(fields, "schoolAddress", ""), MUTED);
        addCentered(table, val(fields, "schoolPhone", ""), MUTED);
        addCentered(table, titleFor(type), SUBTITLE);
        String duplicate = val(fields, "duplicate", "");
        if (!duplicate.isBlank()) {
            addCentered(table, duplicate, WATERMARK);
        }
        return table;
    }

    private PdfPTable body(CertificateType type, Map<String, Object> fields) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(12);
        addLine(table, "Certificate No", val(fields, "certificateNo", "—"));
        addLine(table, "Date of Issue", val(fields, "issuedDate", "—"));
        addLine(table, "Student Name", val(fields, "studentName", ""));
        addLine(table, "Admission No", val(fields, "admissionNo", ""));
        addLine(table, "Date of Birth", val(fields, "dateOfBirth", ""));
        addLine(table, "Class / Section", (val(fields, "className", "") + " " + val(fields, "sectionName", "")).trim());
        addLine(table, "Guardian", val(fields, "guardianName", ""));
        addLine(table, "Academic Year", val(fields, "academicYear", ""));
        addParagraph(table, narrative(type, fields));
        if (type == CertificateType.TC) {
            addLine(table, "Reason for leaving", val(fields, "reason", ""));
        } else if (!val(fields, "reason", "").isBlank()) {
            addLine(table, "Reason for request", val(fields, "reason", ""));
        }
        String conduct = val(fields, "conductRemarks", "");
        if (!conduct.isBlank()) {
            addLine(table, "Conduct remarks", conduct);
        }
        String progress = val(fields, "academicProgress", "");
        if (!progress.isBlank()) {
            addLine(table, "Academic progress", progress);
        }
        String lastExam = val(fields, "lastExamAttended", "");
        if (!lastExam.isBlank()) {
            addLine(table, "Last exam attended", lastExam);
        }
        return table;
    }

    private static String titleFor(CertificateType type) {
        return switch (type) {
            case TC -> "TRANSFER CERTIFICATE";
            case CHARACTER -> "CHARACTER CERTIFICATE";
            case COURSE_COMPLETION -> "COURSE COMPLETION CERTIFICATE";
            default -> "BONAFIDE CERTIFICATE";
        };
    }

    private static String narrative(CertificateType type, Map<String, Object> fields) {
        String name = val(fields, "studentName", "the student");
        String klass = (val(fields, "className", "") + " " + val(fields, "sectionName", "")).trim();
        String year = val(fields, "academicYear", "");
        return switch (type) {
            case TC -> "This is to certify that the student named above was enrolled in this school and is hereby granted a transfer certificate.";
            case CHARACTER -> "This is to certify that " + name + " is a student of this school studying in " + klass
                    + " during the academic year " + year + ". The student's conduct and character have been found satisfactory.";
            case COURSE_COMPLETION -> "This is to certify that " + name + " has satisfactorily completed the course of study in "
                    + klass + " during the academic year " + year + ".";
            default -> "This is to certify that " + name
                    + " (Admission No. " + val(fields, "admissionNo", "—")
                    + "), date of birth " + val(fields, "dateOfBirth", "—")
                    + ", child of " + val(fields, "guardianName", "—")
                    + ", is a bonafide student of this school studying in " + klass
                    + " during the academic year " + year + ".";
        };
    }

    private PdfPTable signatures(Map<String, Object> fields) {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(36);
        addSign(table, val(fields, "teacherSignLabel", "Class Teacher Signature"));
        addSign(table, val(fields, "sealLabel", "School Seal"));
        addSign(table, val(fields, "principalSignLabel", "Principal Signature"));
        return table;
    }

    private Paragraph footer(Map<String, Object> fields) {
        Paragraph p = new Paragraph(val(fields, "footerHtml",
                "This is a computer generated certificate. Principal signature and school seal to be affixed after printing."), MUTED);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(24);
        return p;
    }

    private static void addCentered(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(2);
        table.addCell(cell);
    }

    private static void addLine(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(label + ": " + (value == null || value.isBlank() ? "—" : value), BODY));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        table.addCell(cell);
    }

    private static void addParagraph(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BODY));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(8);
        table.addCell(cell);
    }

    private static void addSign(PdfPTable table, String label) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        Paragraph line = new Paragraph("________________", BODY);
        line.setAlignment(Element.ALIGN_CENTER);
        Paragraph caption = new Paragraph(label, MUTED);
        caption.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(line);
        cell.addElement(caption);
        table.addCell(cell);
    }

    private static String val(Map<String, Object> fields, String key, String fallback) {
        Object value = fields == null ? null : fields.get(key);
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }
}
