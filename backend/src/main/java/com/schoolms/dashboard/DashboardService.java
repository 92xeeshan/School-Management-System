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
import com.schoolms.common.enums.AttendanceStatus;
import com.schoolms.common.enums.NoticeStatus;
import com.schoolms.dashboard.dto.DashboardDto;
import com.schoolms.fee.FeeInstallment;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

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

    @Transactional(readOnly = true)
    public DashboardDto get() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UUID userId = SecurityUtils.currentUserId();
        LocalDate today = LocalDate.now();

        long totalStudents = studentRepository.countBySchoolIdAndStatus(schoolId,
                com.schoolms.common.enums.StudentStatus.ACTIVE);
        long totalTeachers = teacherRepository.findBySchoolIdOrderByFirstNameAsc(schoolId).size();
        long totalClasses = classRepository.findBySchoolIdOrderBySortOrderAsc(schoolId).size();
        long totalSections = sectionRepository.findBySchoolIdOrderByNameAsc(schoolId).size();
        long presentToday = attendanceRepository
                .countBySchoolIdAndAttendanceDateAndStatus(schoolId, today, AttendanceStatus.PRESENT);
        long absentToday = attendanceRepository
                .countBySchoolIdAndAttendanceDateAndStatus(schoolId, today, AttendanceStatus.ABSENT);
        long publishedNotices = noticeRepository.countBySchoolIdAndStatus(schoolId, NoticeStatus.PUBLISHED);
        long unreadNotices = noticeService.unreadCount();
        BigDecimal feesCollected = paymentRepository.sumBySchoolId(schoolId);

        List<DashboardDto.MySectionDto> mySections = mySections(schoolId, userId);
        List<DashboardDto.MyChildDto> myChildren = myChildren(schoolId, userId);
        AttendanceSummaryDto myAttendance = myAttendance(schoolId, userId);
        BigDecimal feeBalance = feeBalance(schoolId, userId);

        return new DashboardDto(totalStudents, totalTeachers, totalClasses, totalSections,
                presentToday, absentToday, publishedNotices, unreadNotices, feesCollected,
                mySections, myChildren, myAttendance, feeBalance);
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
        return new DashboardDto.MySectionDto(ts.getSectionId(),
                section == null ? null : section.getName(), className, count);
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
                present, absent, late, leave, total, Math.round(percentage * 100.0) / 100.0);
    }

    private UUID currentYearId(UUID schoolId) {
        return academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId)
                .map(AcademicYear::getId).orElse(null);
    }
}
