package com.schoolms.dashboard;

import com.schoolms.TestSecurity;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.academics.SectionRepository;
import com.schoolms.academics.StudentEnrollmentRepository;
import com.schoolms.academics.TeacherProfileRepository;
import com.schoolms.academics.TeacherSectionRepository;
import com.schoolms.attendance.AttendanceRepository;
import com.schoolms.award.AwardRepository;
import com.schoolms.common.enums.AttendanceStatus;
import com.schoolms.common.enums.Gender;
import com.schoolms.dashboard.dto.DashboardDto;
import com.schoolms.exam.ExamEntryRepository;
import com.schoolms.exam.ExamMarkRepository;
import com.schoolms.fee.FeeInstallmentRepository;
import com.schoolms.fee.FeePaymentRepository;
import com.schoolms.fee.StudentFeeAssignmentRepository;
import com.schoolms.notice.NoticeRepository;
import com.schoolms.notice.NoticeService;
import com.schoolms.student.GuardianRepository;
import com.schoolms.student.StudentGuardianRepository;
import com.schoolms.student.StudentRepository;
import com.schoolms.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private StudentRepository studentRepository;
    @Mock private TeacherProfileRepository teacherRepository;
    @Mock private SchoolClassRepository classRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private FeePaymentRepository paymentRepository;
    @Mock private NoticeRepository noticeRepository;
    @Mock private NoticeService noticeService;
    @Mock private TeacherSectionRepository teacherSectionRepository;
    @Mock private GuardianRepository guardianRepository;
    @Mock private StudentGuardianRepository studentGuardianRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private StudentEnrollmentRepository enrollmentRepository;
    @Mock private FeeInstallmentRepository installmentRepository;
    @Mock private StudentFeeAssignmentRepository assignmentRepository;
    @Mock private AwardRepository awardRepository;
    @Mock private UserRepository userRepository;
    @Mock private ExamEntryRepository examEntryRepository;
    @Mock private ExamMarkRepository examMarkRepository;

    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(studentRepository, teacherRepository, classRepository,
                sectionRepository, attendanceRepository, paymentRepository, noticeRepository,
                noticeService, teacherSectionRepository, guardianRepository, studentGuardianRepository,
                academicYearRepository, enrollmentRepository, installmentRepository, assignmentRepository,
                awardRepository, userRepository, examEntryRepository, examMarkRepository);
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("ADMIN"),
                java.util.Set.of("DASHBOARD_VIEW"));
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void getComputesCountsGenderAndTrend() {
        stubCommon();
        when(studentRepository.countActiveByGender(TestSecurity.SCHOOL_ID)).thenReturn(List.of(
                new GenderCountRow(Gender.MALE, 3), new GenderCountRow(Gender.FEMALE, 3)));
        LocalDate monday = mostRecentOperationalDay();
        when(attendanceRepository.countByDateAndStatus(eq(TestSecurity.SCHOOL_ID), any(), any()))
                .thenReturn(List.of(
                        new DateStatusRow(monday, AttendanceStatus.PRESENT, 4),
                        new DateStatusRow(monday, AttendanceStatus.ABSENT, 1)));

        DashboardDto dto = service.get();

        assertEquals(6, dto.totalStudents());
        assertEquals(1, dto.totalStaff());
        assertEquals(5, dto.totalAwards());
        assertEquals(new BigDecimal("500"), dto.feesOverdue());
        assertEquals(3, dto.gender().boys());
        assertEquals(3, dto.gender().girls());
        assertEquals(50.0, dto.gender().boysPercent());
        assertEquals(5, dto.attendanceTrend().size());
        DashboardDto.AttendanceTrendPointDto point = dto.attendanceTrend().stream()
                .filter(p -> p.date().equals(monday)).findFirst().orElseThrow();
        assertEquals(5, point.total());
        assertEquals(80.0, point.percentage());
        assertTrue(point.present() > point.absent());
    }

    @Test
    void attendanceTrendSkipsWeekendAndUsesLastFiveOperationalDays() {
        stubCommon();
        when(studentRepository.countActiveByGender(TestSecurity.SCHOOL_ID)).thenReturn(List.of());
        when(attendanceRepository.countByDateAndStatus(eq(TestSecurity.SCHOOL_ID), any(), any()))
                .thenReturn(List.of());

        DashboardDto dto = service.get();

        List<DashboardDto.AttendanceTrendPointDto> trend = dto.attendanceTrend();
        assertEquals(5, trend.size());
        assertTrue(trend.stream().noneMatch(p -> p.date().getDayOfWeek() == DayOfWeek.SATURDAY
                || p.date().getDayOfWeek() == DayOfWeek.SUNDAY));
        for (int i = 1; i < trend.size(); i++) {
            assertTrue(trend.get(i).date().isAfter(trend.get(i - 1).date()));
        }
        assertEquals(0.0, trend.get(trend.size() - 1).percentage());
    }

    @Test
    void adminSeesOverdueAndNoPendingGrading() {
        stubCommon();
        when(studentRepository.countActiveByGender(TestSecurity.SCHOOL_ID)).thenReturn(List.of());
        when(attendanceRepository.countByDateAndStatus(eq(TestSecurity.SCHOOL_ID), any(), any()))
                .thenReturn(List.of());
        when(teacherRepository.findBySchoolIdAndUserId(TestSecurity.SCHOOL_ID, TestSecurity.USER_ID))
                .thenReturn(Optional.empty());

        DashboardDto dto = service.get();

        assertEquals(new BigDecimal("500"), dto.feesOverdue());
        assertEquals(0, dto.pendingGrading());
        assertNotNull(dto.gender());
    }

    private void stubCommon() {
        when(studentRepository.countBySchoolIdAndStatus(eq(TestSecurity.SCHOOL_ID), any())).thenReturn(6L);
        when(teacherRepository.findBySchoolIdOrderByFirstNameAsc(TestSecurity.SCHOOL_ID))
                .thenReturn(List.of(new com.schoolms.academics.TeacherProfile()));
        when(userRepository.countNonTeachingStaff(TestSecurity.SCHOOL_ID)).thenReturn(1L);
        when(classRepository.findBySchoolIdOrderBySortOrderAsc(TestSecurity.SCHOOL_ID)).thenReturn(List.of());
        when(sectionRepository.findBySchoolIdOrderByNameAsc(TestSecurity.SCHOOL_ID)).thenReturn(List.of());
        when(awardRepository.countBySchoolId(TestSecurity.SCHOOL_ID)).thenReturn(5L);
        when(attendanceRepository.countBySchoolIdAndAttendanceDateAndStatus(eq(TestSecurity.SCHOOL_ID), any(), any()))
                .thenReturn(0L);
        when(noticeRepository.countBySchoolIdAndStatus(eq(TestSecurity.SCHOOL_ID), any())).thenReturn(1L);
        when(noticeService.unreadCount()).thenReturn(1L);
        when(paymentRepository.sumBySchoolId(TestSecurity.SCHOOL_ID)).thenReturn(new BigDecimal("1500"));
        when(installmentRepository.sumOverdue(eq(TestSecurity.SCHOOL_ID), any()))
                .thenReturn(new BigDecimal("500"));
        when(awardRepository.findTop10BySchoolIdOrderByPointsDescAwardedDateDesc(TestSecurity.SCHOOL_ID))
                .thenReturn(List.of());
        when(guardianRepository.findBySchoolIdAndUserId(TestSecurity.SCHOOL_ID, TestSecurity.USER_ID))
                .thenReturn(Optional.empty());
        when(academicYearRepository.findBySchoolIdAndCurrentTrue(TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.empty());
    }

    private LocalDate mostRecentOperationalDay() {
        LocalDate cursor = LocalDate.now();
        while (cursor.getDayOfWeek() == DayOfWeek.SATURDAY || cursor.getDayOfWeek() == DayOfWeek.SUNDAY) {
            cursor = cursor.minusDays(1);
        }
        return cursor;
    }

    private record GenderCountRow(Gender gender, long total) implements StudentRepository.GenderCount {
        @Override
        public Gender getGender() {
            return gender;
        }

        @Override
        public long getTotal() {
            return total;
        }
    }

    private record DateStatusRow(LocalDate date, AttendanceStatus status, long total)
            implements AttendanceRepository.DateStatusCount {
        @Override
        public LocalDate getAttendanceDate() {
            return date;
        }

        @Override
        public AttendanceStatus getStatus() {
            return status;
        }

        @Override
        public long getTotal() {
            return total;
        }
    }
}
