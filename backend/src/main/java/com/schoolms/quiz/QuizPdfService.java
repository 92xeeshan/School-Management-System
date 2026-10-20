package com.schoolms.quiz;

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
import com.schoolms.quiz.dto.QuizAnswerDto;
import com.schoolms.quiz.dto.QuizAttemptDto;
import com.schoolms.quiz.dto.QuizDto;
import com.schoolms.quiz.dto.QuizResultRowDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class QuizPdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font MUTED = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);

    public byte[] renderResults(String schoolName, QuizDto quiz, List<QuizResultRowDto> rows) {
        if (quiz == null) {
            throw new BusinessException("quiz.not_found");
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(centered(blank(schoolName), TITLE));
            document.add(centered("QUIZ RESULTS", SUBTITLE));
            document.add(centered(blank(quiz.title()), BODY));
            Paragraph meta = new Paragraph(
                    blank(quiz.className()) + " / " + blank(quiz.sectionName())
                            + "  ·  " + blank(quiz.subjectName())
                            + "  ·  " + blank(quiz.academicYearName()), MUTED);
            meta.setAlignment(Element.ALIGN_CENTER);
            meta.setSpacingAfter(12);
            document.add(meta);

            PdfPTable table = new PdfPTable(new float[] {1.2f, 2.6f, 2.2f, 1.4f, 1.4f, 1.4f, 1.2f});
            table.setWidthPercentage(100);
            headerCell(table, "Roll");
            headerCell(table, "Student");
            headerCell(table, "Admission");
            headerCell(table, "Source");
            headerCell(table, "Score");
            headerCell(table, "%");
            headerCell(table, "Result");
            if (rows != null) {
                for (QuizResultRowDto row : rows) {
                    bodyCell(table, row.rollNumber() == null ? "—" : String.valueOf(row.rollNumber()));
                    bodyCell(table, blank(row.studentName()));
                    bodyCell(table, blank(row.admissionNo()));
                    bodyCell(table, blank(row.source()));
                    bodyCell(table, formatScore(row.score(), row.maxScore()));
                    bodyCell(table, row.percentage() == null ? "—" : row.percentage().stripTrailingZeros().toPlainString() + "%");
                    bodyCell(table, row.score() == null ? "—" : (row.passed() ? "Pass" : "Fail"));
                }
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to render quiz results PDF", e);
            throw new BusinessException("quiz.export_failed");
        }
    }

    public byte[] renderAttempt(String schoolName, QuizDto quiz, QuizAttemptDto attempt) {
        if (attempt == null) {
            throw new BusinessException("quiz.attempt_not_found");
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(centered(blank(schoolName), TITLE));
            document.add(centered("QUIZ RESULT", SUBTITLE));
            document.add(centered(blank(attempt.quizTitle()), BODY));

            PdfPTable meta = new PdfPTable(4);
            meta.setWidthPercentage(100);
            meta.setSpacingBefore(10);
            addMeta(meta, "Name", blank(attempt.studentName()));
            addMeta(meta, "Admission No", blank(attempt.admissionNo()));
            addMeta(meta, "Roll", attempt.rollNumber() == null ? "—" : String.valueOf(attempt.rollNumber()));
            addMeta(meta, "Class / Section",
                    quiz == null ? "—" : blank(quiz.className()) + " - " + blank(quiz.sectionName()));
            addMeta(meta, "Attempt", String.valueOf(attempt.attemptNo()));
            addMeta(meta, "Status", blank(attempt.status()));
            addMeta(meta, "Submitted", attempt.submittedAt() == null ? "—" : DATE.format(attempt.submittedAt()));
            addMeta(meta, "Score", formatScore(attempt.totalScore(), attempt.maxScore()));
            document.add(meta);

            PdfPTable table = new PdfPTable(new float[] {4, 2, 1.4f, 1.4f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(12);
            headerCell(table, "Question");
            headerCell(table, "Response");
            headerCell(table, "Awarded");
            headerCell(table, "Max");
            if (attempt.answers() != null) {
                for (QuizAnswerDto answer : attempt.answers()) {
                    bodyCell(table, blank(answer.prompt()));
                    bodyCell(table, blank(answer.response()));
                    bodyCell(table, answer.awardedMarks() == null ? "—" : strip(answer.awardedMarks()));
                    bodyCell(table, answer.maxMarks() == null ? "—" : strip(answer.maxMarks()));
                }
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to render quiz attempt PDF", e);
            throw new BusinessException("quiz.export_failed");
        }
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

    private static String formatScore(BigDecimal score, BigDecimal max) {
        if (score == null && max == null) {
            return "—";
        }
        return strip(score) + " / " + strip(max);
    }

    private static String strip(BigDecimal value) {
        return value == null ? "—" : value.stripTrailingZeros().toPlainString();
    }

    private static String blank(String value) {
        return value == null ? "" : value;
    }
}
