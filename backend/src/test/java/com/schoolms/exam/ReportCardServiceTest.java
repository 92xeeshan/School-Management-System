package com.schoolms.exam;

import com.schoolms.TestSecurity;
import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.GradeBoundary;
import com.schoolms.academics.GradeBoundaryRepository;
import com.schoolms.academics.GradingScheme;
import com.schoolms.academics.GradingSchemeRepository;
import com.schoolms.academics.SchoolClass;
import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.academics.Section;
import com.schoolms.academics.SectionRepository;
import com.schoolms.academics.StudentEnrollment;
import com.schoolms.academics.StudentEnrollmentRepository;
import com.schoolms.academics.Subject;
import com.schoolms.academics.SubjectRepository;
import com.schoolms.academics.TeacherProfile;
import com.schoolms.academics.TeacherProfileRepository;
import com.schoolms.academics.TeacherSection;
import com.schoolms.academics.TeacherSectionRepository;
import com.schoolms.attendance.Attendance;
import com.schoolms.attendance.AttendanceRepository;
import com.schoolms.award.AwardRepository;
import com.schoolms.common.enums.AttendanceStatus;
import com.schoolms.common.exception.AuthException;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.exam.dto.ReportCardDto;
import com.schoolms.exam.dto.ReportCardExportRequest;
import com.schoolms.exam.dto.ReportCardStudentDto;
import com.schoolms.file.MinioService;
import com.schoolms.school.School;
import com.schoolms.school.SchoolRepository;
import com.schoolms.student.Guardian;
import com.schoolms.student.GuardianRepository;
import com.schoolms.student.Student;
import com.schoolms.student.StudentGuardian;
import com.schoolms.student.StudentGuardianRepository;
import com.schoolms.student.StudentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportCardServiceTest {

    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private SchoolClassRepository classRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private StudentEnrollmentRepository enrollmentRepository;
    @Mock private ExamEntryRepository examEntryRepository;
    @Mock private ExamMarkRepository examMarkRepository;
    @Mock private ReportCardRepository reportCardRepository;
    @Mock private GradingSchemeRepository schemeRepository;
    @Mock private GradeBoundaryRepository boundaryRepository;
    @Mock private TeacherProfileRepository teacherRepository;
    @Mock private TeacherSectionRepository teacherSectionRepository;
    @Mock private SchoolRepository schoolRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private StudentGuardianRepository studentGuardianRepository;
    @Mock private GuardianRepository guardianRepository;
    @Mock private AwardRepository awardRepository;
    @Mock private MinioService minioService;
    @Mock private ReportCardPdfService pdfService;

    private ReportCardService service;
    private final UUID yearId = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private final UUID classId = UUID.fromString("30000000-0000-0000-0000-000000000011");
    private final UUID sectionId = UUID.fromString("30000000-0000-0000-0000-000000000021");
    private final UUID otherSectionId = UUID.fromString("30000000-0000-0000-0000-000000000022");
    private final UUID subjectId = UUID.fromString("30000000-0000-0000-0000-000000000031");
    private final UUID studentId = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private final UUID otherStudentId = UUID.fromString("40000000-0000-0000-0000-000000000002");
    private final UUID studentUserId = UUID.fromString("20000000-0000-0000-0000-000000000010");
    private final UUID entryId = UUID.fromString("80000000-0000-0000-0000-000000000011");

    @BeforeEach
    void setUp() {
        service = new ReportCardService(academicYearRepository, classRepository, sectionRepository, subjectRepository,
                studentRepository, enrollmentRepository, examEntryRepository, examMarkRepository,
                reportCardRepository, schemeRepository, boundaryRepository, teacherRepository,
                teacherSectionRepository, schoolRepository, attendanceRepository, studentGuardianRepository,
                guardianRepository, awardRepository, minioService, pdfService);
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("ADMIN"),
                Set.of("REPORT_CARD_READ", "EXAM_MANAGE"));
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void studentCanLoadOwnPublishedCard() {
        loginStudent();
        stubCardGraph();

        ReportCardDto card = service.mine(yearId, "TERM");

        assertTrue(card.published());
        assertTrue(card.ready());
        assertEquals(studentId, card.studentId());
        assertEquals(1, card.subjects().size());
        assertEquals("Mathematics", card.subjects().get(0).subjectName());
        assertEquals(5, card.attendance().workingDays());
        assertEquals(4, card.attendance().daysPresent());
        assertTrue(card.teacherComment().length() <= 300);
        assertEquals("Rajesh Kumar", card.guardianName());
    }

    @Test
    void studentCannotLoadAnotherStudentCard() {
        loginStudent();
        when(studentRepository.findBySchoolIdAndUserId(TestSecurity.SCHOOL_ID, studentUserId))
                .thenReturn(Optional.of(student(studentId, studentUserId, "Aarav")));

        assertThrows(AuthException.class, () -> service.get(otherStudentId, yearId, "TERM"));
    }

    @Test
    void exportBlockedUntilPublished() {
        stubSectionGraph();
        when(studentRepository.findBySectionAndYear(TestSecurity.SCHOOL_ID, sectionId, yearId))
                .thenReturn(List.of(student(studentId, studentUserId, "Aarav")));
        when(enrollmentRepository.findBySectionIdAndAcademicYearId(sectionId, yearId))
                .thenReturn(List.of(enrollment(studentId, sectionId)));
        when(examEntryRepository.findBySchoolIdAndAcademicYearIdAndSectionIdAndExamTerm(
                TestSecurity.SCHOOL_ID, yearId, sectionId, "TERM")).thenReturn(List.of(entry()));
        when(examMarkRepository.findBySchoolIdAndExamEntryIdIn(eq(TestSecurity.SCHOOL_ID), anyList()))
                .thenReturn(List.of(mark(studentId)));
        when(reportCardRepository.findBySchoolIdAndAcademicYearIdAndSectionIdAndExamTerm(
                TestSecurity.SCHOOL_ID, yearId, sectionId, "TERM")).thenReturn(List.of());
        when(schemeRepository.findBySchoolIdAndAcademicYearIdAndClassIdAndStatus(
                TestSecurity.SCHOOL_ID, yearId, classId, "ACTIVE")).thenReturn(Optional.empty());
        when(teacherSectionRepository.findBySectionIdAndSchoolId(sectionId, TestSecurity.SCHOOL_ID))
                .thenReturn(List.of());
        when(schoolRepository.findById(TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(school()));
        when(attendanceRepository.findByStudentIdInAndAttendanceDateBetween(any(), any(), any()))
                .thenReturn(List.of());
        when(studentGuardianRepository.findWithGuardians(TestSecurity.SCHOOL_ID, studentId))
                .thenReturn(List.of());
        when(awardRepository.findBySchoolIdOrderByAwardedDateDesc(TestSecurity.SCHOOL_ID)).thenReturn(List.of());
        when(subjectRepository.findBySchoolIdOrderByNameAsc(TestSecurity.SCHOOL_ID)).thenReturn(List.of(subject()));

        ReportCardExportRequest request = new ReportCardExportRequest(yearId, "TERM", classId, sectionId, List.of(studentId));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.exportPdf(request));
        assertEquals("report_card.not_published", ex.getCode());
    }

    @Test
    void attendanceMatchesMarkedDays() {
        loginStudent();
        stubCardGraph();

        ReportCardDto card = service.mine(yearId, "TERM");

        assertEquals(5, card.attendance().workingDays());
        assertEquals(4, card.attendance().daysPresent());
        assertEquals(1, card.attendance().daysAbsent());
        assertEquals("80", card.attendance().percent().stripTrailingZeros().toPlainString());
    }

    @Test
    void teacherCannotAccessUnassignedSection() {
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("TEACHER"),
                Set.of("REPORT_CARD_READ", "EXAM_READ"));
        UUID teacherId = UUID.fromString("30000000-0000-0000-0000-000000000041");
        TeacherProfile teacher = new TeacherProfile();
        teacher.setId(teacherId);
        teacher.setSchoolId(TestSecurity.SCHOOL_ID);
        teacher.setUserId(TestSecurity.USER_ID);
        when(academicYearRepository.findByIdAndSchoolId(yearId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(year()));
        stubSection();
        when(teacherRepository.findBySchoolIdAndUserId(TestSecurity.SCHOOL_ID, TestSecurity.USER_ID))
                .thenReturn(Optional.of(teacher));
        TeacherSection assignment = new TeacherSection();
        assignment.setTeacherId(teacherId);
        assignment.setSectionId(otherSectionId);
        assignment.setSchoolId(TestSecurity.SCHOOL_ID);
        when(teacherSectionRepository.findByTeacherIdAndSchoolId(teacherId, TestSecurity.SCHOOL_ID))
                .thenReturn(List.of(assignment));

        assertThrows(AuthException.class, () -> service.roster(yearId, classId, sectionId, "TERM"));
    }

    @Test
    void rosterShowsUnpublishedUntilRemarksExist() {
        stubSectionGraph();
        when(studentRepository.findBySectionAndYear(TestSecurity.SCHOOL_ID, sectionId, yearId))
                .thenReturn(List.of(student(studentId, studentUserId, "Aarav")));
        when(enrollmentRepository.findBySectionIdAndAcademicYearId(sectionId, yearId))
                .thenReturn(List.of(enrollment(studentId, sectionId)));
        when(examEntryRepository.findBySchoolIdAndAcademicYearIdAndSectionIdAndExamTerm(
                TestSecurity.SCHOOL_ID, yearId, sectionId, "TERM")).thenReturn(List.of());
        when(subjectRepository.findBySchoolIdOrderByNameAsc(TestSecurity.SCHOOL_ID)).thenReturn(List.of(subject()));
        when(reportCardRepository.findBySchoolIdAndAcademicYearIdAndSectionIdAndExamTerm(
                TestSecurity.SCHOOL_ID, yearId, sectionId, "TERM")).thenReturn(List.of());
        when(schemeRepository.findBySchoolIdAndAcademicYearIdAndClassIdAndStatus(
                TestSecurity.SCHOOL_ID, yearId, classId, "ACTIVE")).thenReturn(Optional.empty());
        when(teacherSectionRepository.findBySectionIdAndSchoolId(sectionId, TestSecurity.SCHOOL_ID))
                .thenReturn(List.of());
        when(schoolRepository.findById(TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(school()));
        when(attendanceRepository.findByStudentIdInAndAttendanceDateBetween(any(), any(), any()))
                .thenReturn(List.of());
        when(studentGuardianRepository.findWithGuardians(TestSecurity.SCHOOL_ID, studentId))
                .thenReturn(List.of());
        when(awardRepository.findBySchoolIdOrderByAwardedDateDesc(TestSecurity.SCHOOL_ID)).thenReturn(List.of());

        List<ReportCardStudentDto> roster = service.roster(yearId, classId, sectionId, "TERM");

        assertEquals(1, roster.size());
        assertFalse(roster.get(0).published());
        assertFalse(roster.get(0).ready());
    }

    private void loginStudent() {
        TestSecurity.login(studentUserId, TestSecurity.SCHOOL_ID, List.of("STUDENT"), Set.of("REPORT_CARD_READ"));
    }

    private void stubCardGraph() {
        when(academicYearRepository.findByIdAndSchoolId(yearId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(year()));
        when(studentRepository.findBySchoolIdAndUserId(TestSecurity.SCHOOL_ID, studentUserId))
                .thenReturn(Optional.of(student(studentId, studentUserId, "Aarav")));
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(studentId, yearId))
                .thenReturn(Optional.of(enrollment(studentId, sectionId)));
        stubSection();
        when(classRepository.findByIdAndSchoolId(classId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(schoolClass()));
        when(studentRepository.findBySectionAndYear(TestSecurity.SCHOOL_ID, sectionId, yearId))
                .thenReturn(List.of(student(studentId, studentUserId, "Aarav")));
        when(enrollmentRepository.findBySectionIdAndAcademicYearId(sectionId, yearId))
                .thenReturn(List.of(enrollment(studentId, sectionId)));
        when(examEntryRepository.findBySchoolIdAndAcademicYearIdAndSectionIdAndExamTerm(
                TestSecurity.SCHOOL_ID, yearId, sectionId, "TERM")).thenReturn(List.of(entry()));
        when(subjectRepository.findBySchoolIdOrderByNameAsc(TestSecurity.SCHOOL_ID)).thenReturn(List.of(subject()));
        when(examMarkRepository.findBySchoolIdAndExamEntryIdIn(eq(TestSecurity.SCHOOL_ID), anyList()))
                .thenReturn(List.of(mark(studentId)));
        when(reportCardRepository.findBySchoolIdAndAcademicYearIdAndSectionIdAndExamTerm(
                TestSecurity.SCHOOL_ID, yearId, sectionId, "TERM")).thenReturn(List.of(stored()));
        GradingScheme scheme = new GradingScheme();
        scheme.setId(UUID.fromString("90000000-0000-0000-0000-000000000001"));
        scheme.setPassPercent(new BigDecimal("33"));
        when(schemeRepository.findBySchoolIdAndAcademicYearIdAndClassIdAndStatus(
                TestSecurity.SCHOOL_ID, yearId, classId, "ACTIVE")).thenReturn(Optional.of(scheme));
        GradeBoundary a = new GradeBoundary();
        a.setLabel("A");
        a.setMinPercent(new BigDecimal("80"));
        a.setMaxPercent(new BigDecimal("100"));
        when(boundaryRepository.findBySchemeIdAndSchoolIdOrderBySortOrderAsc(scheme.getId(), TestSecurity.SCHOOL_ID))
                .thenReturn(List.of(a));
        when(teacherSectionRepository.findBySectionIdAndSchoolId(sectionId, TestSecurity.SCHOOL_ID))
                .thenReturn(List.of());
        when(schoolRepository.findById(TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(school()));
        when(attendanceRepository.findByStudentIdInAndAttendanceDateBetween(any(), any(), any()))
                .thenReturn(attendanceRows());
        StudentGuardian link = new StudentGuardian();
        Guardian guardian = new Guardian();
        guardian.setFirstName("Rajesh");
        guardian.setLastName("Kumar");
        link.setGuardian(guardian);
        link.setPrimary(true);
        when(studentGuardianRepository.findWithGuardians(TestSecurity.SCHOOL_ID, studentId))
                .thenReturn(List.of(link));
        when(awardRepository.findBySchoolIdOrderByAwardedDateDesc(TestSecurity.SCHOOL_ID)).thenReturn(List.of());
    }

    private void stubSectionGraph() {
        when(academicYearRepository.findByIdAndSchoolId(yearId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(year()));
        stubSection();
        when(classRepository.findByIdAndSchoolId(classId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(schoolClass()));
    }

    private void stubSection() {
        when(sectionRepository.findByIdAndSchoolId(sectionId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(section()));
    }

    private AcademicYear year() {
        AcademicYear year = new AcademicYear();
        year.setId(yearId);
        year.setSchoolId(TestSecurity.SCHOOL_ID);
        year.setName("2025-26");
        year.setStartDate(LocalDate.of(2025, 4, 1));
        year.setEndDate(LocalDate.of(2026, 3, 31));
        year.setCurrent(true);
        return year;
    }

    private SchoolClass schoolClass() {
        SchoolClass klass = new SchoolClass();
        klass.setId(classId);
        klass.setSchoolId(TestSecurity.SCHOOL_ID);
        klass.setName("Class 5");
        return klass;
    }

    private Section section() {
        Section section = new Section();
        section.setId(sectionId);
        section.setSchoolId(TestSecurity.SCHOOL_ID);
        section.setClassId(classId);
        section.setName("A");
        return section;
    }

    private Student student(UUID id, UUID userId, String firstName) {
        Student student = new Student();
        student.setId(id);
        student.setSchoolId(TestSecurity.SCHOOL_ID);
        student.setUserId(userId);
        student.setAdmissionNo("ADM0001");
        student.setFirstName(firstName);
        student.setLastName("Kumar");
        student.setDateOfBirth(LocalDate.of(2014, 5, 10));
        return student;
    }

    private StudentEnrollment enrollment(UUID student, UUID section) {
        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudentId(student);
        enrollment.setSectionId(section);
        enrollment.setAcademicYearId(yearId);
        enrollment.setSchoolId(TestSecurity.SCHOOL_ID);
        enrollment.setRollNumber(1);
        enrollment.setStatus("ACTIVE");
        return enrollment;
    }

    private ExamEntry entry() {
        ExamEntry entry = new ExamEntry();
        entry.setId(entryId);
        entry.setSchoolId(TestSecurity.SCHOOL_ID);
        entry.setAcademicYearId(yearId);
        entry.setClassId(classId);
        entry.setSectionId(sectionId);
        entry.setSubjectId(subjectId);
        entry.setExamTerm("TERM");
        entry.setMaxTheory(new BigDecimal("80"));
        entry.setMaxPractical(BigDecimal.ZERO);
        entry.setMaxAssignment(new BigDecimal("20"));
        return entry;
    }

    private ExamMark mark(UUID student) {
        ExamMark mark = new ExamMark();
        mark.setExamEntryId(entryId);
        mark.setStudentId(student);
        mark.setTheory(new BigDecimal("72"));
        mark.setPractical(BigDecimal.ZERO);
        mark.setAssignment(new BigDecimal("18"));
        mark.setTotal(new BigDecimal("90"));
        mark.setPercentage(new BigDecimal("90"));
        mark.setGradeLabel("A");
        mark.setStatus("SUBMITTED");
        return mark;
    }

    private ReportCard stored() {
        ReportCard card = new ReportCard();
        card.setStudentId(studentId);
        card.setAcademicYearId(yearId);
        card.setExamTerm("TERM");
        card.setTeacherComment("Aarav is a sincere learner who participates well in class discussions.");
        card.setPrincipalComment("A promising student. Keep up the balanced effort.");
        card.setBehaviourConduct("EXCELLENT");
        card.setBehaviourDiscipline("GOOD");
        card.setBehaviourPunctuality("EXCELLENT");
        card.setCoCurricular("Mathematics Olympiad Champion");
        card.setPublished(true);
        return card;
    }

    private List<Attendance> attendanceRows() {
        return List.of(
                attendance(LocalDate.of(2025, 4, 1), AttendanceStatus.PRESENT),
                attendance(LocalDate.of(2025, 4, 2), AttendanceStatus.PRESENT),
                attendance(LocalDate.of(2025, 4, 3), AttendanceStatus.LATE),
                attendance(LocalDate.of(2025, 4, 4), AttendanceStatus.PRESENT),
                attendance(LocalDate.of(2025, 4, 5), AttendanceStatus.ABSENT)
        );
    }

    private Attendance attendance(LocalDate date, AttendanceStatus status) {
        Attendance row = new Attendance();
        row.setStudentId(studentId);
        row.setSectionId(sectionId);
        row.setAttendanceDate(date);
        row.setStatus(status);
        return row;
    }

    private Subject subject() {
        Subject subject = new Subject();
        subject.setId(subjectId);
        subject.setSchoolId(TestSecurity.SCHOOL_ID);
        subject.setName("Mathematics");
        subject.setCode("MATH");
        return subject;
    }

    private School school() {
        School school = new School();
        school.setId(TestSecurity.SCHOOL_ID);
        school.setName("Demo Public School");
        school.setAddress("1 Education Road");
        school.setPhone("0000000000");
        school.setCode("DEMO");
        return school;
    }
}
