package com.schoolms.exam;

import com.schoolms.TestSecurity;
import com.schoolms.common.exception.AuthException;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.exam.dto.ClassAnalyticsDto;
import com.schoolms.exam.dto.ReportCardAttendanceDto;
import com.schoolms.exam.dto.ReportCardBehaviourDto;
import com.schoolms.exam.dto.ReportCardDto;
import com.schoolms.exam.dto.ReportCardStudentDto;
import com.schoolms.exam.dto.ReportCardSubjectDto;
import com.schoolms.exam.dto.StudentTrendDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamAnalyticsServiceTest {

    @Mock private ReportCardService reportCardService;

    private ExamAnalyticsService service;
    private final UUID yearId = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private final UUID classId = UUID.fromString("30000000-0000-0000-0000-000000000011");
    private final UUID sectionId = UUID.fromString("30000000-0000-0000-0000-000000000021");
    private final UUID studentId = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private final UUID otherStudentId = UUID.fromString("40000000-0000-0000-0000-000000000002");
    private final UUID subjectId = UUID.fromString("30000000-0000-0000-0000-000000000031");

    @BeforeEach
    void setUp() {
        service = new ExamAnalyticsService(reportCardService);
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("ADMIN"),
                Set.of("EXAM_READ", "EXAM_MANAGE", "REPORT_CARD_READ"));
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void classAnalyticsUsesPublishedCardsOnly() {
        when(reportCardService.roster(yearId, classId, sectionId, "TERM")).thenReturn(List.of(
                rosterRow(studentId, "Aarav", true, new BigDecimal("90")),
                rosterRow(otherStudentId, "Draft", false, new BigDecimal("40"))
        ));
        when(reportCardService.get(studentId, yearId, "TERM"))
                .thenReturn(card(studentId, "Aarav", "TERM", true, new BigDecimal("90"), "PASS"));

        ClassAnalyticsDto dto = service.classAnalytics(yearId, classId, sectionId, "TERM");

        assertEquals(2, dto.studentCount());
        assertEquals(1, dto.publishedCount());
        assertEquals(new BigDecimal("90.00"), dto.classAverage());
        assertEquals(new BigDecimal("100.00"), dto.passPercent());
        assertEquals(1, dto.topPerformers().size());
        assertEquals(studentId, dto.topPerformers().get(0).studentId());
        assertEquals(1, dto.subjectDistribution().size());
        assertEquals("Mathematics", dto.subjectDistribution().get(0).subjectName());
    }

    @Test
    void learnerCannotLoadClassAnalytics() {
        TestSecurity.login(UUID.fromString("20000000-0000-0000-0000-000000000010"), TestSecurity.SCHOOL_ID,
                List.of("STUDENT"), Set.of("REPORT_CARD_READ"));

        assertThrows(AuthException.class, () -> service.classAnalytics(yearId, classId, sectionId, "TERM"));
    }

    @Test
    void staffTrendRequiresStudent() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.studentTrend(yearId, null));
        assertEquals("exam_analytics.student_required", ex.getCode());
    }

    @Test
    void studentTrendIncludesPublishedTermsOnly() {
        when(reportCardService.get(eq(studentId), eq(yearId), any()))
                .thenAnswer(invocation -> {
                    String term = invocation.getArgument(2);
                    boolean published = "TERM".equals(term) || "FINAL".equals(term);
                    return card(studentId, "Aarav", term, published,
                            "FINAL".equals(term) ? new BigDecimal("80") : new BigDecimal("90"), "PASS");
                });

        StudentTrendDto trend = service.studentTrend(yearId, studentId);

        assertEquals(studentId, trend.studentId());
        assertEquals(2, trend.points().size());
        assertTrue(trend.points().stream().allMatch(point -> point.published()));
        assertEquals("TERM", trend.points().get(0).examTerm());
        assertEquals("FINAL", trend.points().get(1).examTerm());
    }

    private ReportCardStudentDto rosterRow(UUID id, String name, boolean published, BigDecimal percent) {
        return new ReportCardStudentDto(id, name, "ADM0001", 1, null, true, published, percent, "A");
    }

    private ReportCardDto card(UUID id, String name, String term, boolean published, BigDecimal percent, String result) {
        ReportCardSubjectDto subject = new ReportCardSubjectDto(
                subjectId, "Mathematics", "MATH",
                new BigDecimal("72"), new BigDecimal("80"),
                BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("18"), new BigDecimal("20"),
                percent, new BigDecimal("100"), percent, "A", null);
        return new ReportCardDto(
                id, name, "ADM0001", 1, LocalDate.of(2014, 5, 10), "Rajesh Kumar", null,
                classId, "Class 5", sectionId, "A", yearId, "2025-26", term, true, published,
                "Demo Public School", "1 Education Road", "0000000000", "", "CBSE", "Asha Sharma",
                percent, new BigDecimal("100"), percent, "A", result,
                new ReportCardAttendanceDto(5, 4, 1, 0, 0, new BigDecimal("80"), LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31)),
                List.of(subject),
                new ReportCardBehaviourDto("GOOD", "GOOD", "GOOD", null),
                "Good", "Keep it up");
    }
}
