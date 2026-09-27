package com.schoolms.notification;

import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.StudentEnrollment;
import com.schoolms.academics.StudentEnrollmentRepository;
import com.schoolms.attendance.Attendance;
import com.schoolms.attendance.AttendanceRepository;
import com.schoolms.common.enums.AttendanceStatus;
import com.schoolms.exam.ExamEntry;
import com.schoolms.exam.ExamEntryRepository;
import com.schoolms.fee.FeeInstallment;
import com.schoolms.fee.FeeInstallmentRepository;
import com.schoolms.fee.StudentFeeAssignment;
import com.schoolms.fee.StudentFeeAssignmentRepository;
import com.schoolms.school.School;
import com.schoolms.school.SchoolRepository;
import com.schoolms.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationTriggerService {

    static final double ATTENDANCE_THRESHOLD = 75.0;
    private static final DateTimeFormatter DUE_DATE = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);

    private final NotificationService notificationService;
    private final NotificationRecipientResolver recipients;
    private final SchoolRepository schoolRepository;
    private final AcademicYearRepository academicYearRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final ExamEntryRepository examEntryRepository;
    private final FeeInstallmentRepository feeInstallmentRepository;
    private final StudentFeeAssignmentRepository assignmentRepository;

    @Transactional
    public void onMarksheetSubmitted(UUID schoolId, String className, String sectionName, UUID actorId) {
        String label = classLabel(className, sectionName);
        String actor = recipients.displayName(actorId);
        String message = label + " marks submitted by " + actor + " for review.";
        notificationService.notifyUsers(
                schoolId,
                recipients.adminReviewers(schoolId),
                "Marks submitted for review",
                message,
                "EXAM",
                "/downloads/marksheet",
                null);
    }

    @Transactional
    public void onMarksheetRejected(UUID schoolId, String className, String sectionName, UUID submittedBy,
                                    UUID sectionId, String reason) {
        String label = classLabel(className, sectionName);
        String message = label + " marks require revision.";
        if (reason != null && !reason.isBlank()) {
            message = message + " " + reason.trim();
        }
        Set<UUID> teacherIds = new LinkedHashSet<>();
        if (submittedBy != null) {
            teacherIds.add(submittedBy);
        }
        teacherIds.addAll(recipients.classTeachers(schoolId, sectionId));
        notificationService.notifyUsers(
                schoolId,
                teacherIds,
                "Marks require revision",
                message,
                "EXAM",
                "/examinations/marks",
                null);
    }

    @Transactional
    public void onMarksheetPublished(UUID schoolId, String examTerm, Collection<UUID> studentIds) {
        List<UUID> learnerIds = recipients.studentAndParents(schoolId, studentIds);
        notificationService.notifyUsers(
                schoolId,
                learnerIds,
                "Mark sheet published",
                "Mark sheet published for " + termLabel(examTerm) + ".",
                "ASSIGNMENT",
                "/downloads",
                null);
    }

    @Transactional
    public void onAttendanceMarked(UUID schoolId, UUID academicYearId, Collection<UUID> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return;
        }
        AcademicYear year = academicYearRepository.findByIdAndSchoolId(academicYearId, schoolId).orElse(null);
        LocalDate from = year == null ? LocalDate.now().withDayOfYear(1) : year.getStartDate();
        LocalDate to = LocalDate.now();
        for (UUID studentId : studentIds) {
            notifyAttendanceIfLow(schoolId, studentId, from, to);
        }
    }

    public void runDailyReminders() {
        TenantContext.setBypassRls(true);
        try {
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);
            LocalDate upcoming = today.plusDays(3);
            for (School school : schoolRepository.findAll()) {
                if (school.getId() == null) {
                    continue;
                }
                TenantContext.setSchoolId(school.getId());
                sendMarksDeadlineReminders(school.getId(), tomorrow);
                sendAttendanceWarnings(school.getId(), today);
                sendFeeReminders(school.getId(), today, upcoming);
            }
        } finally {
            TenantContext.clear();
        }
    }

    private void sendMarksDeadlineReminders(UUID schoolId, LocalDate tomorrow) {
        List<ExamEntry> due = examEntryRepository.findByLockedFalseAndManualUnlockFalseAndEntryDeadline(tomorrow);
        Map<String, ExamEntry> unique = new HashMap<>();
        for (ExamEntry entry : due) {
            if (!schoolId.equals(entry.getSchoolId())) {
                continue;
            }
            unique.putIfAbsent(entry.getSectionId() + ":" + entry.getExamTerm(), entry);
        }
        for (ExamEntry entry : unique.values()) {
            List<UUID> teacherIds = recipients.sectionTeachers(
                    schoolId, entry.getClassId(), entry.getSectionId(), entry.getSubjectId());
            notificationService.notifyUsers(
                    schoolId,
                    teacherIds,
                    "Marks entry deadline",
                    "Reminder: Marks submission for " + termLabel(entry.getExamTerm()) + " closes in 24 hours.",
                    "EXAM",
                    "/examinations/marks",
                    "MARKS_DEADLINE:" + entry.getSectionId() + ":" + entry.getExamTerm() + ":" + tomorrow);
        }
    }

    private void sendAttendanceWarnings(UUID schoolId, LocalDate today) {
        AcademicYear year = academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId).orElse(null);
        if (year == null) {
            return;
        }
        List<StudentEnrollment> enrollments = enrollmentRepository
                .findBySchoolIdAndAcademicYearIdAndStatus(schoolId, year.getId(), "ACTIVE");
        for (StudentEnrollment enrollment : enrollments) {
            notifyAttendanceIfLow(schoolId, enrollment.getStudentId(), year.getStartDate(), today);
        }
    }

    private void notifyAttendanceIfLow(UUID schoolId, UUID studentId, LocalDate from, LocalDate to) {
        List<Attendance> records = attendanceRepository
                .findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(studentId, from, to);
        if (records.isEmpty()) {
            return;
        }
        long present = records.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE)
                .count();
        double percentage = Math.round((present * 10000.0) / records.size()) / 100.0;
        if (percentage >= ATTENDANCE_THRESHOLD) {
            return;
        }
        String message = "Attendance warning: Current attendance is " + strip(percentage) + "%.";
        notificationService.notifyUsers(
                schoolId,
                recipients.studentAndParents(schoolId, studentId),
                "Attendance warning",
                message,
                "ATTENDANCE",
                "/attendance",
                "ATTENDANCE_LOW:" + studentId + ":" + from + ":" + to.getMonthValue());
    }

    private void sendFeeReminders(UUID schoolId, LocalDate today, LocalDate upcoming) {
        List<FeeInstallment> due = feeInstallmentRepository.findDueBetween(schoolId, today.minusYears(1), upcoming);
        Map<UUID, List<FeeInstallment>> byAssignment = new HashMap<>();
        for (FeeInstallment installment : due) {
            if (installment.getAmountDue() == null || installment.getAmountPaid() == null) {
                continue;
            }
            if (installment.getAmountDue().compareTo(installment.getAmountPaid()) <= 0) {
                continue;
            }
            byAssignment.computeIfAbsent(installment.getStudentFeeAssignmentId(), id -> new ArrayList<>())
                    .add(installment);
        }
        for (Map.Entry<UUID, List<FeeInstallment>> entry : byAssignment.entrySet()) {
            StudentFeeAssignment assignment = assignmentRepository.findByIdAndSchoolId(entry.getKey(), schoolId)
                    .orElse(null);
            if (assignment == null) {
                continue;
            }
            List<FeeInstallment> ordered = feeInstallmentRepository
                    .findByStudentFeeAssignmentIdOrderByDueDateAsc(assignment.getId());
            for (FeeInstallment installment : entry.getValue()) {
                int number = installmentNumber(ordered, installment.getId());
                boolean overdue = installment.getDueDate() != null && installment.getDueDate().isBefore(today);
                BigDecimal outstanding = installment.getAmountDue().subtract(installment.getAmountPaid());
                String dueText = installment.getDueDate() == null ? "" : installment.getDueDate().format(DUE_DATE);
                String message = overdue
                        ? "Fee Due: Installment #" + number + " of " + strip(outstanding) + " is overdue."
                        : "Fee Due: Installment #" + number + " of " + strip(outstanding) + " is due on " + dueText + ".";
                String kind = overdue ? "OVERDUE" : "DUE";
                notificationService.notifyUsers(
                        schoolId,
                        recipients.studentAndParents(schoolId, assignment.getStudentId()),
                        overdue ? "Fee overdue" : "Fee due",
                        message,
                        "FEE",
                        "/fees",
                        "FEE:" + kind + ":" + installment.getId());
            }
        }
    }

    static String classLabel(String className, String sectionName) {
        String klass = className == null ? "Class" : className.trim();
        String section = sectionName == null ? "" : sectionName.trim();
        if (klass.toLowerCase(Locale.ENGLISH).startsWith("class ")) {
            klass = klass.substring(6).trim();
        }
        if (section.isBlank()) {
            return "Class " + klass;
        }
        return "Class " + klass + "-" + section;
    }

    static String termLabel(String examTerm) {
        if (examTerm == null || examTerm.isBlank()) {
            return "Examinations";
        }
        return switch (examTerm.toUpperCase(Locale.ENGLISH)) {
            case "QUIZ" -> "Quiz Examinations";
            case "UNIT" -> "Unit Examinations";
            case "MIDTERM" -> "Mid-Term Examinations";
            case "TERM" -> "Term Examinations";
            case "FINAL" -> "Final Examinations";
            case "CONTINUOUS" -> "Continuous Assessment";
            default -> examTerm + " Examinations";
        };
    }

    private static int installmentNumber(List<FeeInstallment> ordered, UUID installmentId) {
        for (int i = 0; i < ordered.size(); i++) {
            if (installmentId.equals(ordered.get(i).getId())) {
                return i + 1;
            }
        }
        return 1;
    }

    private static String strip(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static String strip(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
