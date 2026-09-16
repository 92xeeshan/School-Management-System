package com.schoolms.dashboard;

import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.SchoolClass;
import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.academics.Section;
import com.schoolms.academics.SectionRepository;
import com.schoolms.academics.StudentEnrollmentRepository;
import com.schoolms.academics.TeacherProfile;
import com.schoolms.academics.TeacherProfileRepository;
import com.schoolms.academics.TeacherSection;
import com.schoolms.academics.TeacherSectionRepository;
import com.schoolms.attendance.AttendanceRepository;
import com.schoolms.attendance.dto.AttendanceSummaryDto;
import com.schoolms.award.Award;
import com.schoolms.award.AwardRepository;
import com.schoolms.common.enums.AttendanceStatus;
import com.schoolms.common.enums.Gender;
import com.schoolms.common.enums.NoticeStatus;
import com.schoolms.common.enums.StudentStatus;
import com.schoolms.dashboard.dto.DashboardDto;
import com.schoolms.exam.ExamEntryRepository;
import com.schoolms.exam.ExamMarkRepository;
import com.schoolms.fee.FeeInstallmentRepository;
import com.schoolms.fee.FeePaymentRepository;
import com.schoolms.fee.StudentFeeAssignmentRepository;
import com.schoolms.notice.NoticeRepository;
import com.schoolms.notice.NoticeService;
import com.schoolms.security.SecurityUtils;
import com.schoolms.student.GuardianRepository;
import com.schoolms.student.Student;
import com.schoolms.student.StudentGuardianRepository;
import com.schoolms.student.StudentRepository;
import com.schoolms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int TREND_DAYS = 5;

    private final StudentRepository studentRepository;
    private final TeacherProfileRepository teacherRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final AttendanceRepository attendanceRepository;
    private final FeePaymentRepository paymentRepository;
    private final NoticeRepository noticeRepository;
    private final NoticeService noticeService;
    private final TeacherSectionRepository teacherSectionRepository;
    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final AcademicYearRepository academicYearRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final FeeInstallmentRepository installmentRepository;
    private final StudentFeeAssignmentRepository assignmentRepository;
    private final AwardRepository awardRepository;
    private final UserRepository userRepository;
    private final ExamEntryRepository examEntryRepository;
    private final ExamMarkRepository examMarkRepository;

    @Transactional(readOnly = true)
    public DashboardDto get() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UUID userId = SecurityUtils.currentUserId();
        LocalDate today = LocalDate.now();

        long totalStudents = studentRepository.countBySchoolIdAndStatus(schoolId, StudentStatus.ACTIVE);
        long totalTeachers = teacherRepository.findBySchoolIdOrderByFirstNameAsc(schoolId).size();
        long totalStaff = userRepository.countNonTeachingStaff(schoolId);
        long totalClasses = classRepository.findBySchoolIdOrderBySortOrderAsc(schoolId).size();
        long totalSections = sectionRepository.findBySchoolIdOrderByNameAsc(schoolId).size();
        long totalAwards = awardRepository.countBySchoolId(schoolId);
        long presentToday = attendanceRepository
                .countBySchoolIdAndAttendanceDateAndStatus(schoolId, today, AttendanceStatus.PRESENT);
        long absentToday = attendanceRepository
                .countBySchoolIdAndAttendanceDateAndStatus(schoolId, today, AttendanceStatus.ABSENT);
        long publishedNotices = noticeRepository.countBySchoolIdAndStatus(schoolId, NoticeStatus.PUBLISHED);
        long unreadNotices = noticeService.unreadCount();
        BigDecimal feesCollected = paymentRepository.sumBySchoolId(schoolId);
        BigDecimal feesOverdue = installmentRepository.sumOverdue(schoolId, today);

        List<DashboardDto.MySectionDto> mySections = mySections(schoolId, userId);
        List<DashboardDto.MyChildDto> myChildren = myChildren(schoolId, userId);
        AttendanceSummaryDto myAttendance = myAttendance(schoolId, userId);
        BigDecimal feeBalance = feeBalance(schoolId, userId);

        return new DashboardDto(totalStudents, totalTeachers, totalStaff, totalClasses, totalSections,
                totalAwards, presentToday, absentToday, publishedNotices, unreadNotices,
                feesCollected, feesOverdue, genderBreakdown(schoolId),
                attendanceTrend(schoolId, today), starStudents(schoolId),
                mySections, myChildren, myAttendance, feeBalance, pendingGrading(schoolId, userId));
    }

    private DashboardDto.GenderBreakdownDto genderBreakdown(UUID schoolId) {
        long boys = 0;
        long girls = 0;
        long other = 0;
        for (StudentRepository.GenderCount row : studentRepository.countActiveByGender(schoolId)) {
            if (row.getGender() == Gender.MALE) {
                boys += row.getTotal();
            } else if (row.getGender() == Gender.FEMALE) {
                girls += row.getTotal();
            } else {
                other += row.getTotal();
            }
        }
        long total = boys + girls + other;
        return new DashboardDto.GenderBreakdownDto(boys, girls, other, total,
                percent(boys, total), percent(girls, total), percent(other, total));
    }

    private List<DashboardDto.AttendanceTrendPointDto> attendanceTrend(UUID schoolId, LocalDate today) {
        List<LocalDate> days = operationalDays(today, TREND_DAYS);
        if (days.isEmpty()) {
            return List.of();
        }
        LocalDate from = days.get(0);
        LocalDate to = days.get(days.size() - 1);
        Map<LocalDate, Map<AttendanceStatus, Long>> grouped = new java.util.HashMap<>();
        for (AttendanceRepository.DateStatusCount row : attendanceRepository
                .countByDateAndStatus(schoolId, from, to)) {
            grouped.computeIfAbsent(row.getAttendanceDate(), d -> new EnumMap<>(AttendanceStatus.class))
                    .put(row.getStatus(), row.getTotal());
        }
        List<DashboardDto.AttendanceTrendPointDto> trend = new ArrayList<>();
        for (LocalDate day : days) {
            Map<AttendanceStatus, Long> counts = grouped.getOrDefault(day, Map.of());
            long present = counts.getOrDefault(AttendanceStatus.PRESENT, 0L);
            long absent = counts.getOrDefault(AttendanceStatus.ABSENT, 0L);
            long late = counts.getOrDefault(AttendanceStatus.LATE, 0L);
            long leave = counts.getOrDefault(AttendanceStatus.LEAVE, 0L);
            long total = present + absent + late + leave;
            double percentage = total == 0 ? 0.0 : round1((present + late) * 100.0 / total);
            trend.add(new DashboardDto.AttendanceTrendPointDto(day, present, absent, late, leave,
                    total, percentage));
        }
        return trend;
    }

    private List<LocalDate> operationalDays(LocalDate today, int count) {
        List<LocalDate> days = new ArrayList<>();
        LocalDate cursor = today;
        while (days.size() < count) {
            DayOfWeek dow = cursor.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                days.add(cursor);
            }
            cursor = cursor.minusDays(1);
        }
        java.util.Collections.reverse(days);
        return days;
    }

    private List<DashboardDto.StarStudentDto> starStudents(UUID schoolId) {
        UUID yearId = currentYearId(schoolId);
        return awardRepository.findTop10BySchoolIdOrderByPointsDescAwardedDateDesc(schoolId).stream()
                .filter(a -> a.getStudentId() != null)
                .map(a -> toStarStudent(a, schoolId, yearId))
                .filter(java.util.Objects::nonNull)
                .limit(5)
                .toList();
    }

    private DashboardDto.StarStudentDto toStarStudent(Award award, UUID schoolId, UUID yearId) {
        Student student = studentRepository.findByIdAndSchoolId(award.getStudentId(), schoolId).orElse(null);
        if (student == null) {
            return null;
        }
        String className = null;
        String sectionName = null;
        if (yearId != null) {
            Section section = enrollmentRepository.findByStudentIdAndAcademicYearId(student.getId(), yearId)
                    .flatMap(enrollment -> sectionRepository
                            .findByIdAndSchoolId(enrollment.getSectionId(), schoolId))
                    .orElse(null);
            if (section != null) {
                sectionName = section.getName();
                className = classRepository.findByIdAndSchoolId(section.getClassId(), schoolId)
                        .map(SchoolClass::getName).orElse(null);
            }
        }
        return new DashboardDto.StarStudentDto(student.getId(), student.getDisplayName(),
                student.getAdmissionNo(), className, sectionName, award.getTitle(),
                award.getCategory(), award.getBadge(), award.getPoints(), award.getAwardedDate());
    }

    private long pendingGrading(UUID schoolId, UUID userId) {
        List<UUID> sectionIds = teacherRepository.findBySchoolIdAndUserId(schoolId, userId)
                .map(TeacherProfile::getId)
                .map(teacherId -> teacherSectionRepository.findByTeacherIdAndSchoolId(teacherId, schoolId))
                .orElse(List.of())
                .stream().map(TeacherSection::getSectionId).distinct().toList();
        if (sectionIds.isEmpty()) {
            return 0;
        }
        return examEntryRepository.countPendingForSections(schoolId, sectionIds);
    }

    private List<DashboardDto.MySectionDto> mySections(UUID schoolId, UUID userId) {
        return teacherRepository.findBySchoolIdAndUserId(schoolId, userId)
                .map(teacher -> teacherSectionRepository.findByTeacherIdAndSchoolId(teacher.getId(), schoolId)
                        .stream().map(ts -> toSectionDto(ts, schoolId)).toList())
                .orElse(List.of());
    }

    private DashboardDto.MySectionDto toSectionDto(TeacherSection ts, UUID schoolId) {
        Section section = sectionRepository.findByIdAndSchoolId(ts.getSectionId(), schoolId).orElse(null);
        String className = section == null ? null
                : classRepository.findByIdAndSchoolId(section.getClassId(), schoolId)
                        .map(SchoolClass::getName).orElse(null);
        UUID yearId = currentYearId(schoolId);
        long count = yearId == null ? 0
                : studentRepository.findBySectionAndYear(schoolId, ts.getSectionId(), yearId).size();
        Double average = round1OrNull(examMarkRepository.avgPercentageBySection(schoolId, ts.getSectionId()));
        return new DashboardDto.MySectionDto(ts.getSectionId(),
                section == null ? null : section.getName(), className, count, average);
    }

    private List<DashboardDto.MyChildDto> myChildren(UUID schoolId, UUID userId) {
        return guardianRepository.findBySchoolIdAndUserId(schoolId, userId)
                .map(guardian -> studentGuardianRepository.findWithStudents(schoolId, guardian.getId()).stream()
                        .map(sg -> toChildDto(sg.getStudent(), schoolId)).toList())
                .orElse(List.of());
    }

    private DashboardDto.MyChildDto toChildDto(Student student, UUID schoolId) {
        UUID yearId = currentYearId(schoolId);
        String className = null;
        String sectionName = null;
        if (yearId != null) {
            Section section = enrollmentRepository.findByStudentIdAndAcademicYearId(student.getId(), yearId)
                    .flatMap(enrollment -> sectionRepository
                            .findByIdAndSchoolId(enrollment.getSectionId(), schoolId))
                    .orElse(null);
            if (section != null) {
                sectionName = section.getName();
                className = classRepository.findByIdAndSchoolId(section.getClassId(), schoolId)
                        .map(SchoolClass::getName).orElse(null);
            }
        }
        return new DashboardDto.MyChildDto(student.getId(), student.getDisplayName(),
                student.getAdmissionNo(), className, sectionName);
    }

    private AttendanceSummaryDto myAttendance(UUID schoolId, UUID userId) {
        UUID yearId = currentYearId(schoolId);
        if (yearId == null) {
            return null;
        }
        return studentRepository.findBySchoolIdAndUserId(schoolId, userId)
                .map(student -> {
                    AcademicYear year = academicYearRepository.findByIdAndSchoolId(yearId, schoolId).orElse(null);
                    if (year == null) {
                        return null;
                    }
                    return studentAttendanceSummary(student.getId(), year.getStartDate(), LocalDate.now());
                })
                .orElse(null);
    }

    private BigDecimal feeBalance(UUID schoolId, UUID userId) {
        UUID studentId = studentRepository.findBySchoolIdAndUserId(schoolId, userId)
                .map(Student::getId).orElse(null);
        if (studentId == null) {
            return null;
        }
        return assignmentRepository.findByStudentIdAndSchoolId(studentId, schoolId).stream()
                .flatMap(a -> installmentRepository
                        .findByStudentFeeAssignmentIdOrderByDueDateAsc(a.getId()).stream())
                .map(i -> i.getAmountDue().subtract(i.getAmountPaid()))
                .filter(v -> v.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private AttendanceSummaryDto studentAttendanceSummary(UUID studentId, LocalDate from, LocalDate to) {
        List<com.schoolms.attendance.Attendance> records = attendanceRepository
                .findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(studentId, from, to);
        long present = records.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        long absent = records.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
        long late = records.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
        long leave = records.stream().filter(a -> a.getStatus() == AttendanceStatus.LEAVE).count();
        long total = records.size();
        double percentage = total == 0 ? 0.0 : (double) (present + late) * 100.0 / total;
        String studentName = studentRepository.findById(studentId)
                .map(Student::getDisplayName).orElse(null);
        return new AttendanceSummaryDto(studentId, studentName,
                present, absent, late, leave, total, round1(percentage));
    }

    private UUID currentYearId(UUID schoolId) {
        return academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId)
                .map(AcademicYear::getId).orElse(null);
    }

    private static double percent(long value, long total) {
        return total == 0 ? 0.0 : round1(value * 100.0 / total);
    }

    private static double round1(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static Double round1OrNull(Double value) {
        return value == null ? null : round1(value);
    }
}
