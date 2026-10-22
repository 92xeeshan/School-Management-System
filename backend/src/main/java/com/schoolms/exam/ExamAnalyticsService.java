package com.schoolms.exam;

import com.schoolms.common.exception.AuthException;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.exam.dto.ClassAnalyticsDto;
import com.schoolms.exam.dto.ReportCardDto;
import com.schoolms.exam.dto.ReportCardOptionsDto;
import com.schoolms.exam.dto.ReportCardStudentDto;
import com.schoolms.exam.dto.ReportCardSubjectDto;
import com.schoolms.exam.dto.StudentTrendDto;
import com.schoolms.exam.dto.StudentTrendPointDto;
import com.schoolms.exam.dto.SubjectDistributionDto;
import com.schoolms.exam.dto.TopPerformerDto;
import com.schoolms.security.SecurityUtils;
import com.schoolms.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExamAnalyticsService {

    private static final int TOP_LIMIT = 10;

    private final ReportCardService reportCardService;

    @Transactional(readOnly = true)
    public ReportCardOptionsDto options(UUID studentId) {
        return reportCardService.options(studentId);
    }

    @Transactional(readOnly = true)
    public ClassAnalyticsDto classAnalytics(UUID academicYearId, UUID classId, UUID sectionId, String examTerm) {
        if (isLearner(SecurityUtils.currentPrincipal())) {
            throw AuthException.accessDenied();
        }
        List<ReportCardStudentDto> roster = reportCardService.roster(academicYearId, classId, sectionId, examTerm);
        List<ReportCardDto> published = roster.stream()
                .filter(ReportCardStudentDto::published)
                .map(row -> reportCardService.get(row.studentId(), academicYearId, examTerm))
                .filter(ReportCardDto::published)
                .toList();
        BigDecimal classAverage = average(published.stream().map(ReportCardDto::percentage).toList());
        long passCount = published.stream().filter(card -> "PASS".equals(card.result())).count();
        BigDecimal passPercent = published.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(passCount * 100.0 / published.size()).setScale(2, RoundingMode.HALF_UP);
        String yearName = published.isEmpty() ? "" : published.get(0).academicYearName();
        String className = published.isEmpty() ? "" : published.get(0).className();
        String sectionName = published.isEmpty() ? "" : published.get(0).sectionName();
        UUID resolvedClassId = published.isEmpty() ? classId : published.get(0).classId();
        UUID resolvedSectionId = published.isEmpty() ? sectionId : published.get(0).sectionId();
        UUID resolvedYearId = published.isEmpty() ? academicYearId : published.get(0).academicYearId();
        String term = published.isEmpty() ? examTerm : published.get(0).examTerm();
        return new ClassAnalyticsDto(
                resolvedYearId,
                yearName,
                resolvedClassId,
                className,
                resolvedSectionId,
                sectionName,
                term,
                roster.size(),
                published.size(),
                classAverage,
                passPercent,
                subjectDistribution(published),
                topPerformers(published));
    }

    @Transactional(readOnly = true)
    public StudentTrendDto studentTrend(UUID academicYearId, UUID studentId) {
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        if (!isLearner(principal) && studentId == null) {
            throw new BusinessException("exam_analytics.student_required");
        }
        ReportCardDto seed = isLearner(principal) && studentId == null
                ? reportCardService.mine(academicYearId, ReportCardService.EXAM_TERMS.get(0), null)
                : reportCardService.get(studentId, academicYearId, ReportCardService.EXAM_TERMS.get(0));
        List<StudentTrendPointDto> points = new ArrayList<>();
        for (String term : ReportCardService.EXAM_TERMS) {
            ReportCardDto card = reportCardService.get(seed.studentId(), academicYearId, term);
            if (!card.published()) {
                continue;
            }
            points.add(new StudentTrendPointDto(
                    card.examTerm(),
                    true,
                    card.percentage(),
                    card.overallGrade(),
                    card.result()));
        }
        return new StudentTrendDto(
                seed.studentId(),
                seed.studentName(),
                seed.admissionNo(),
                seed.academicYearId(),
                seed.academicYearName(),
                seed.classId(),
                seed.className(),
                seed.sectionId(),
                seed.sectionName(),
                points);
    }

    private static List<SubjectDistributionDto> subjectDistribution(List<ReportCardDto> cards) {
        Map<UUID, SubjectAccumulator> bySubject = new LinkedHashMap<>();
        for (ReportCardDto card : cards) {
            for (ReportCardSubjectDto subject : card.subjects()) {
                if (subject.percentage() == null) {
                    continue;
                }
                SubjectAccumulator acc = bySubject.computeIfAbsent(
                        subject.subjectId(),
                        id -> new SubjectAccumulator(subject.subjectName()));
                acc.total = acc.total.add(subject.percentage());
                acc.count += 1;
            }
        }
        return bySubject.entrySet().stream()
                .map(entry -> new SubjectDistributionDto(
                        entry.getKey(),
                        entry.getValue().name,
                        entry.getValue().count,
                        average(entry.getValue().total, entry.getValue().count)))
                .toList();
    }

    private static List<TopPerformerDto> topPerformers(List<ReportCardDto> cards) {
        List<ReportCardDto> ranked = cards.stream()
                .sorted(Comparator.comparing(ReportCardDto::percentage, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(TOP_LIMIT)
                .toList();
        List<TopPerformerDto> rows = new ArrayList<>();
        int rank = 1;
        for (ReportCardDto card : ranked) {
            rows.add(new TopPerformerDto(
                    rank++,
                    card.studentId(),
                    card.studentName(),
                    card.admissionNo(),
                    card.rollNumber(),
                    card.percentage(),
                    card.overallGrade(),
                    card.result()));
        }
        return rows;
    }

    private static BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> present = values.stream().filter(value -> value != null).toList();
        return average(present.stream().reduce(BigDecimal.ZERO, BigDecimal::add), present.size());
    }

    private static BigDecimal average(BigDecimal total, int count) {
        if (count == 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private static boolean isLearner(UserPrincipal principal) {
        return (principal.hasRole("STUDENT") || principal.hasRole("PARENT"))
                && !principal.hasRole("ADMIN") && !principal.hasRole("SUPER_ADMIN")
                && !principal.hasRole("TEACHER");
    }

    private static final class SubjectAccumulator {
        private final String name;
        private BigDecimal total = BigDecimal.ZERO;
        private int count;

        private SubjectAccumulator(String name) {
            this.name = name;
        }
    }
}
