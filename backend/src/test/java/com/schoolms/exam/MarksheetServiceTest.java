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
import com.schoolms.common.enums.Gender;
import com.schoolms.common.exception.AuthException;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.exam.dto.MarksheetActionRequest;
import com.schoolms.exam.dto.MarksheetDto;
import com.schoolms.exam.dto.MarksheetExportRequest;
import com.schoolms.exam.dto.MarksheetPublishRequest;
import com.schoolms.exam.dto.MarksheetStudentDto;
import com.schoolms.file.MinioService;
import com.schoolms.school.School;
import com.schoolms.school.SchoolRepository;
import com.schoolms.student.GuardianRepository;
import com.schoolms.student.Student;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarksheetServiceTest {

    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private SchoolClassRepository classRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private StudentEnrollmentRepository enrollmentRepository;
    @Mock private ExamEntryRepository examEntryRepository;
    @Mock private ExamMarkRepository examMarkRepository;
    @Mock private MarksheetRepository marksheetRepository;
    @Mock private GradingSchemeRepository schemeRepository;
    @Mock private GradeBoundaryRepository boundaryRepository;
    @Mock private TeacherProfileRepository teacherRepository;
    @Mock private TeacherSectionRepository teacherSectionRepository;
    @Mock private SchoolRepository schoolRepository;
    @Mock private GuardianRepository guardianRepository;
    @Mock private StudentGuardianRepository studentGuardianRepository;
    @Mock private MinioService minioService;
    @Mock private MarksheetPdfService pdfService;

    private MarksheetService service;
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
        service = new MarksheetService(academicYearRepository, classRepository, sectionRepository, subjectRepository,
                studentRepository, enrollmentRepository, examEntryRepository, examMarkRepository,
                marksheetRepository, schemeRepository, boundaryRepository, teacherRepository,
                teacherSectionRepository, schoolRepository, guardianRepository, studentGuardianRepository,
                minioService, pdfService);
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("ADMIN"),
                Set.of("MARKSHEET_READ", "MARKSHEET_MANAGE"));
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void studentCanLoadOwnPublishedMarksheet() {
        loginStudent();
        stubCardGraph(true, Gender.MALE, "90", "A", "4.00");

        MarksheetDto card = service.mine(yearId, "TERM");

        assertTrue(card.published());
        assertTrue(card.ready());
        assertEquals(studentId, card.studentId());
        assertEquals("PASS", card.result());
        assertEquals("A", card.overallGrade());
        assertEquals("90", card.percentage().stripTrailingZeros().toPlainString());
        assertEquals("4", card.gpa().stripTrailingZeros().toPlainString());
        assertEquals("MS-2025-26-ADM0001-TERM", card.serialNo());
        assertEquals("MALE", card.gender());
        assertTrue(card.photoPlaceholder());
        assertEquals("Marksheet_1_TERM.pdf", service.downloadFilename(card));
    }

    @Test
    void femalePlaceholderWhenPhotoMissing() {
        loginStudent();
        stubCardGraph(true, Gender.FEMALE, "90", "A", "4.00");

        MarksheetDto card = service.mine(yearId, "TERM");

        assertEquals("FEMALE", card.gender());
        assertTrue(card.photoPlaceholder());
    }

    @Test
    void studentCannotLoadAnotherStudentMarksheet() {
        loginStudent();
        when(studentRepository.findBySchoolIdAndUserId(TestSecurity.SCHOOL_ID, studentUserId))
                .thenReturn(Optional.of(student(studentId, studentUserId, "Aarav", Gender.MALE)));

        assertThrows(AuthException.class, () -> service.get(otherStudentId, yearId, "TERM"));
    }

    @Test
    void exportBlockedUntilPublished() {
        stubSectionGraph();
        stubRosterWithoutRecord();

        MarksheetExportRequest request = new MarksheetExportRequest(yearId, "TERM", classId, sectionId, List.of(studentId));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.exportPdf(request));
        assertEquals("marksheet.not_published", ex.getCode());
    }

    @Test
    void failWhenBelowPassPercent() {
        loginStudent();
        stubCardGraph(true, Gender.MALE, "30", "F", "0.00");

        MarksheetDto card = service.mine(yearId, "TERM");

        assertEquals("FAIL", card.result());
        assertEquals("F", card.overallGrade());
        assertEquals("30", card.percentage().stripTrailingZeros().toPlainString());
        assertEquals("0", card.gpa().stripTrailingZeros().toPlainString());
    }

    @Test
    void teacherCannotAccessUnassignedSection() {
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("TEACHER"),
                Set.of("MARKSHEET_READ"));
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

        assertThrows(AuthException.class, () -> service.roster(yearId, classId, sectionId, "TERM", null, null));
    }

    @Test
    void rosterFiltersByAdmissionNo() {
        stubSectionGraph();
        stubRosterWithoutRecord();

        List<MarksheetStudentDto> all = service.roster(yearId, classId, sectionId, "TERM", null, null);
        List<MarksheetStudentDto> filtered = service.roster(yearId, classId, sectionId, "TERM", "adm0001", null);
        List<MarksheetStudentDto> none = service.roster(yearId, classId, sectionId, "TERM", "zzz", null);

        assertEquals(1, all.size());
        assertEquals(1, filtered.size());
        assertEquals(0, none.size());
    }

    @Test
    void publishRequiresPendingApproval() {
        stubSectionGraph();
        stubRosterWithoutRecord();

        BusinessException ex = assertThrows(BusinessException.class, () -> service.publish(new MarksheetPublishRequest(
                yearId, "TERM", classId, sectionId, List.of(studentId), true, true)));
        assertEquals("marksheet.invalid_transition", ex.getCode());
    }

    @Test
    void approvePublishesAndLocks() {
        stubSectionGraph();
        stubRosterWithoutRecord();
        when(marksheetRepository.findBySchoolIdAndAcademicYearIdAndSectionIdAndExamTerm(
                TestSecurity.SCHOOL_ID, yearId, sectionId, "TERM")).thenReturn(List.of(stored("PENDING_APPROVAL")));
        when(marksheetRepository.save(org.mockito.ArgumentMatchers.any(Marksheet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<MarksheetStudentDto> rows = service.approve(new MarksheetActionRequest(
                yearId, "TERM", classId, sectionId, List.of(studentId), null, true));

        assertEquals(1, rows.size());
        assertTrue(rows.get(0).published());
        assertTrue(rows.get(0).locked());
        assertEquals("PUBLISHED", rows.get(0).status());
        assertEquals(1, rows.get(0).classRank());
    }

    @Test
    void studentUnpublishedCardIsRedacted() {
        loginStudent();
        stubCardGraph(false, Gender.MALE, "90", "A", "4.00");

        MarksheetDto card = service.mine(yearId, "TERM");

        assertFalse(card.published());
        assertEquals("DRAFT", card.status());
        assertTrue(card.subjects().isEmpty());
        assertEquals("0", card.percentage().stripTrailingZeros().toPlainString());
    }

    private void loginStudent() {
        TestSecurity.login(studentUserId, TestSecurity.SCHOOL_ID, List.of("STUDENT"), Set.of("MARKSHEET_READ"));
    }

    private void stubCardGraph(boolean published, Gender gender, String percent, String label, String gpa) {
        when(academicYearRepository.findByIdAndSchoolId(yearId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(year()));
        when(studentRepository.findBySchoolIdAndUserId(TestSecurity.SCHOOL_ID, studentUserId))
                .thenReturn(Optional.of(student(studentId, studentUserId, "Aarav", gender)));
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(studentId, yearId))
                .thenReturn(Optional.of(enrollment(studentId, sectionId)));
        stubSection();
        when(classRepository.findByIdAndSchoolId(classId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(schoolClass()));
        when(studentRepository.findBySectionAndYear(TestSecurity.SCHOOL_ID, sectionId, yearId))
                .thenReturn(List.of(student(studentId, studentUserId, "Aarav", gender)));
        when(enrollmentRepository.findBySectionIdAndAcademicYearId(sectionId, yearId))
                .thenReturn(List.of(enrollment(studentId, sectionId)));
        when(examEntryRepository.findBySchoolIdAndAcademicYearIdAndSectionIdAndExamTerm(
                TestSecurity.SCHOOL_ID, yearId, sectionId, "TERM")).thenReturn(List.of(entry()));
        when(subjectRepository.findBySchoolIdOrderByNameAsc(TestSecurity.SCHOOL_ID)).thenReturn(List.of(subject()));
        when(examMarkRepository.findBySchoolIdAndExamEntryIdIn(eq(TestSecurity.SCHOOL_ID), anyList()))
                .thenReturn(List.of(mark(studentId, percent, label)));
        when(marksheetRepository.findBySchoolIdAndAcademicYearIdAndSectionIdAndExamTerm(
                TestSecurity.SCHOOL_ID, yearId, sectionId, "TERM"))
                .thenReturn(List.of(stored(published ? "PUBLISHED" : "DRAFT")));
        stubScheme(label, gpa, percent);
        when(schoolRepository.findById(TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(school()));
    }

    private void stubRosterWithoutRecord() {
        when(studentRepository.findBySectionAndYear(TestSecurity.SCHOOL_ID, sectionId, yearId))
                .thenReturn(List.of(student(studentId, studentUserId, "Aarav", Gender.MALE)));
        when(enrollmentRepository.findBySectionIdAndAcademicYearId(sectionId, yearId))
                .thenReturn(List.of(enrollment(studentId, sectionId)));
        when(examEntryRepository.findBySchoolIdAndAcademicYearIdAndSectionIdAndExamTerm(
                TestSecurity.SCHOOL_ID, yearId, sectionId, "TERM")).thenReturn(List.of(entry()));
        when(examMarkRepository.findBySchoolIdAndExamEntryIdIn(eq(TestSecurity.SCHOOL_ID), anyList()))
                .thenReturn(List.of(mark(studentId, "90", "A")));
        when(marksheetRepository.findBySchoolIdAndAcademicYearIdAndSectionIdAndExamTerm(
                TestSecurity.SCHOOL_ID, yearId, sectionId, "TERM")).thenReturn(List.of());
        stubScheme("A", "4.00", "90");
        when(schoolRepository.findById(TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(school()));
        when(subjectRepository.findBySchoolIdOrderByNameAsc(TestSecurity.SCHOOL_ID)).thenReturn(List.of(subject()));
    }

    private void stubScheme(String label, String gpa, String percent) {
        GradingScheme scheme = new GradingScheme();
        scheme.setId(UUID.fromString("90000000-0000-0000-0000-000000000001"));
        scheme.setPassPercent(new BigDecimal("33"));
        scheme.setPassMarks(new BigDecimal("33"));
        when(schemeRepository.findBySchoolIdAndAcademicYearIdAndClassIdAndStatus(
                TestSecurity.SCHOOL_ID, yearId, classId, "ACTIVE")).thenReturn(Optional.of(scheme));
        GradeBoundary boundary = new GradeBoundary();
        boundary.setLabel(label);
        BigDecimal value = new BigDecimal(percent);
        boundary.setMinPercent(value);
        boundary.setMaxPercent(value);
        boundary.setGpaValue(new BigDecimal(gpa));
        when(boundaryRepository.findBySchemeIdAndSchoolIdOrderBySortOrderAsc(scheme.getId(), TestSecurity.SCHOOL_ID))
                .thenReturn(List.of(boundary));
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

    private Student student(UUID id, UUID userId, String firstName, Gender gender) {
        Student student = new Student();
        student.setId(id);
        student.setSchoolId(TestSecurity.SCHOOL_ID);
        student.setUserId(userId);
        student.setAdmissionNo("ADM0001");
        student.setFirstName(firstName);
        student.setLastName("Kumar");
        student.setDateOfBirth(LocalDate.of(2014, 5, 10));
        student.setGender(gender);
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

    private ExamMark mark(UUID student, String percent, String grade) {
        ExamMark mark = new ExamMark();
        mark.setExamEntryId(entryId);
        mark.setStudentId(student);
        BigDecimal value = new BigDecimal(percent);
        mark.setTheory(value.subtract(new BigDecimal("10")));
        mark.setPractical(BigDecimal.ZERO);
        mark.setAssignment(new BigDecimal("10"));
        mark.setTotal(value);
        mark.setPercentage(value);
        mark.setGradeLabel(grade);
        mark.setStatus("SUBMITTED");
        return mark;
    }

    private Marksheet stored(String status) {
        Marksheet row = new Marksheet();
        row.setStudentId(studentId);
        row.setAcademicYearId(yearId);
        row.setExamTerm("TERM");
        row.setSerialNo("MS-2025-26-ADM0001-TERM");
        row.setStatus(status);
        row.setPublished("PUBLISHED".equals(status));
        row.setLocked(false);
        row.setIssuedAt(LocalDate.of(2026, 3, 31));
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
