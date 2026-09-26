package com.schoolms.exam;

import com.schoolms.TestSecurity;
import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.ClassSubjectRepository;
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
import com.schoolms.academics.TeacherProfileRepository;
import com.schoolms.academics.TeacherSectionRepository;
import com.schoolms.academics.TeacherSubjectRepository;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.exam.dto.MarksGridDto;
import com.schoolms.exam.dto.MarksLockRequest;
import com.schoolms.exam.dto.MarksSaveRequest;
import com.schoolms.student.Student;
import com.schoolms.student.StudentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamMarksServiceTest {

    @Mock private ExamEntryRepository examEntryRepository;
    @Mock private ExamMarkRepository examMarkRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private SchoolClassRepository classRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private ClassSubjectRepository classSubjectRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private StudentEnrollmentRepository enrollmentRepository;
    @Mock private GradingSchemeRepository schemeRepository;
    @Mock private GradeBoundaryRepository boundaryRepository;
    @Mock private TeacherProfileRepository teacherRepository;
    @Mock private TeacherSectionRepository teacherSectionRepository;
    @Mock private TeacherSubjectRepository teacherSubjectRepository;
    @Mock private MarksheetService marksheetService;

    private ExamMarksService service;
    private UUID yearId;
    private UUID classId;
    private UUID sectionId;
    private UUID subjectId;
    private UUID studentId;
    private ExamEntry entry;

    @BeforeEach
    void setUp() {
        service = new ExamMarksService(examEntryRepository, examMarkRepository, academicYearRepository,
                classRepository, sectionRepository, subjectRepository, classSubjectRepository,
                studentRepository, enrollmentRepository, schemeRepository, boundaryRepository,
                teacherRepository, teacherSectionRepository, teacherSubjectRepository, marksheetService);
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("ADMIN"),
                Set.of("EXAM_READ", "EXAM_MARK", "EXAM_MANAGE"));
        yearId = UUID.randomUUID();
        classId = UUID.randomUUID();
        sectionId = UUID.randomUUID();
        subjectId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        entry = new ExamEntry();
        entry.setId(UUID.randomUUID());
        entry.setSchoolId(TestSecurity.SCHOOL_ID);
        entry.setAcademicYearId(yearId);
        entry.setClassId(classId);
        entry.setSectionId(sectionId);
        entry.setSubjectId(subjectId);
        entry.setExamTerm("TERM");
        entry.setMaxTheory(new BigDecimal("80"));
        entry.setMaxPractical(BigDecimal.ZERO);
        entry.setMaxAssignment(new BigDecimal("20"));
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void saveRejectsTheoryAboveMax() {
        stubContext(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.save(saveRequest(
                new BigDecimal("81"), null, new BigDecimal("10"), false)));
        assertEquals("marks.theory_exceeds", ex.getCode());
        verify(examMarkRepository, never()).save(any());
    }

    @Test
    void saveComputesTotalPercentAndLetterGrade() {
        stubContext(false);
        when(examMarkRepository.findByExamEntryIdAndStudentId(entry.getId(), studentId)).thenReturn(Optional.empty());
        ArgumentCaptor<ExamMark> captor = ArgumentCaptor.forClass(ExamMark.class);
        when(examMarkRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        when(examEntryRepository.findByIdAndSchoolId(entry.getId(), TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(entry));
        when(examMarkRepository.findByExamEntryIdAndSchoolId(entry.getId(), TestSecurity.SCHOOL_ID))
                .thenAnswer(invocation -> captor.getAllValues());

        MarksGridDto grid = service.save(saveRequest(new BigDecimal("72"), null, new BigDecimal("18"), true));

        ExamMark saved = captor.getValue();
        assertEquals(new BigDecimal("90.00"), saved.getTotal());
        assertEquals(new BigDecimal("90.00"), saved.getPercentage());
        assertEquals("A", saved.getGradeLabel());
        assertEquals("SUBMITTED", saved.getStatus());
        assertEquals(1, grid.rows().size());
    }

    @Test
    void saveRejectsWhenLocked() {
        entry.setLocked(true);
        stubContext(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.save(saveRequest(
                new BigDecimal("40"), null, new BigDecimal("10"), false)));
        assertEquals("marks.locked", ex.getCode());
    }

    @Test
    void saveRejectsWhenMarksheetLocked() {
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("TEACHER"),
                Set.of("EXAM_READ", "EXAM_MARK"));
        stubContext(false);
        when(marksheetService.sectionTermLocked(TestSecurity.SCHOOL_ID, yearId, sectionId, "TERM"))
                .thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.save(saveRequest(
                new BigDecimal("40"), null, new BigDecimal("10"), false)));
        assertEquals("marksheet.locked", ex.getCode());
        verify(examMarkRepository, never()).save(any());
    }

    @Test
    void deadlineLocksEditingUnlessAdminUnlocks() {
        entry.setEntryDeadline(LocalDate.now().minusDays(1));
        stubContext(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.save(saveRequest(
                new BigDecimal("40"), null, new BigDecimal("10"), false)));
        assertEquals("marks.locked", ex.getCode());
    }

    @Test
    void adminUnlockAllowsEditingAfterDeadline() {
        entry.setEntryDeadline(LocalDate.now().minusDays(1));
        stubContext(false);
        when(examMarkRepository.findByExamEntryIdAndStudentId(entry.getId(), studentId)).thenReturn(Optional.empty());
        when(examMarkRepository.save(any(ExamMark.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(examEntryRepository.findByIdAndSchoolId(entry.getId(), TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(entry));
        when(examMarkRepository.findByExamEntryIdAndSchoolId(entry.getId(), TestSecurity.SCHOOL_ID))
                .thenReturn(List.of());

        service.lock(new MarksLockRequest(yearId, classId, sectionId, subjectId, "TERM", false,
                LocalDate.now().minusDays(1)));
        assertTrue(entry.isManualUnlock());

        MarksGridDto grid = service.save(saveRequest(new BigDecimal("40"), null, new BigDecimal("10"), false));
        assertEquals(entry.getId(), grid.examEntryId());
    }

    @Test
    void importFlagsUnknownStudent() {
        stubContext(false);
        when(examMarkRepository.findByExamEntryIdAndSchoolId(entry.getId(), TestSecurity.SCHOOL_ID))
                .thenReturn(List.of());
        when(examEntryRepository.findByIdAndSchoolId(entry.getId(), TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(entry));
        MockMultipartFile file = new MockMultipartFile("file", "marks.csv", "text/csv",
                "Admission No,Roll,Student Name,Theory,Practical,Assignment,Attendance,Remarks\nMISSING,1,Ghost,10,0,5,PRESENT,\n"
                        .getBytes(StandardCharsets.UTF_8));

        var result = service.importFile(yearId, classId, sectionId, subjectId, "TERM", file);
        assertEquals(1, result.total());
        assertEquals(0, result.succeeded());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).contains("marks.unknown_student"));
    }

    private MarksSaveRequest saveRequest(BigDecimal theory, BigDecimal practical, BigDecimal assignment,
                                         boolean submit) {
        return new MarksSaveRequest(yearId, classId, sectionId, subjectId, "TERM", submit,
                List.of(new MarksSaveRequest.MarkRow(studentId, theory, practical, assignment, "PRESENT", null)));
    }

    private void stubContext(boolean createEntry) {
        AcademicYear year = new AcademicYear();
        year.setId(yearId);
        year.setName("2025-26");
        year.setCurrent(true);
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(classId);
        schoolClass.setName("Class 5");
        Section section = new Section();
        section.setId(sectionId);
        section.setClassId(classId);
        section.setName("A");
        Subject subject = new Subject();
        subject.setId(subjectId);
        subject.setName("English");
        Student student = new Student();
        student.setId(studentId);
        student.setAdmissionNo("ADM-1");
        student.setFirstName("Ali");
        student.setLastName("Khan");
        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudentId(studentId);
        enrollment.setSectionId(sectionId);
        enrollment.setAcademicYearId(yearId);
        enrollment.setRollNumber(1);
        GradingScheme scheme = new GradingScheme();
        scheme.setId(UUID.randomUUID());
        scheme.setMaxMarks(new BigDecimal("100"));
        scheme.setPassMarks(new BigDecimal("33"));
        scheme.setScaleType("LETTER");
        scheme.setName("Class letter scheme");
        GradeBoundary a = new GradeBoundary();
        a.setLabel("A");
        a.setMinPercent(new BigDecimal("80"));
        a.setMaxPercent(new BigDecimal("100"));
        a.setSortOrder(1);
        GradeBoundary f = new GradeBoundary();
        f.setLabel("F");
        f.setMinPercent(BigDecimal.ZERO);
        f.setMaxPercent(new BigDecimal("32.99"));
        f.setSortOrder(2);

        when(academicYearRepository.findByIdAndSchoolId(yearId, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(year));
        when(classRepository.findByIdAndSchoolId(classId, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(schoolClass));
        when(sectionRepository.findByIdAndSchoolId(sectionId, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(section));
        when(subjectRepository.findByIdAndSchoolId(subjectId, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(subject));
        when(classSubjectRepository.existsByClassIdAndSubjectId(classId, subjectId)).thenReturn(true);
        when(examEntryRepository.findBySchoolIdAndAcademicYearIdAndSectionIdAndSubjectIdAndExamTerm(
                TestSecurity.SCHOOL_ID, yearId, sectionId, subjectId, "TERM")).thenReturn(Optional.of(entry));
        when(schemeRepository.findBySchoolIdAndAcademicYearIdAndClassIdAndStatus(
                TestSecurity.SCHOOL_ID, yearId, classId, "ACTIVE")).thenReturn(Optional.of(scheme));
        when(boundaryRepository.findBySchemeIdAndSchoolIdOrderBySortOrderAsc(scheme.getId(), TestSecurity.SCHOOL_ID))
                .thenReturn(List.of(a, f));
        when(studentRepository.findBySectionAndYear(TestSecurity.SCHOOL_ID, sectionId, yearId))
                .thenReturn(List.of(student));
        when(enrollmentRepository.findBySectionIdAndAcademicYearId(sectionId, yearId)).thenReturn(List.of(enrollment));
    }
}
