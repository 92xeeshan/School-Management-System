package com.schoolms.exam;

import com.schoolms.TestSecurity;
import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
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
import com.schoolms.common.exception.AuthException;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.exam.dto.AdmitCardDto;
import com.schoolms.exam.dto.AdmitCardExportRequest;
import com.schoolms.exam.dto.AdmitCardStudentDto;
import com.schoolms.file.MinioService;
import com.schoolms.school.School;
import com.schoolms.school.SchoolRepository;
import com.schoolms.student.Student;
import com.schoolms.student.StudentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmitCardServiceTest {

    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private SchoolClassRepository classRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private StudentEnrollmentRepository enrollmentRepository;
    @Mock private ExamScheduleRepository scheduleRepository;
    @Mock private TeacherProfileRepository teacherRepository;
    @Mock private TeacherSectionRepository teacherSectionRepository;
    @Mock private SchoolRepository schoolRepository;
    @Mock private MinioService minioService;
    @Mock private AdmitCardPdfService pdfService;

    private AdmitCardService service;
    private final UUID yearId = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private final UUID classId = UUID.fromString("30000000-0000-0000-0000-000000000011");
    private final UUID sectionId = UUID.fromString("30000000-0000-0000-0000-000000000021");
    private final UUID otherSectionId = UUID.fromString("30000000-0000-0000-0000-000000000022");
    private final UUID subjectId = UUID.fromString("30000000-0000-0000-0000-000000000031");
    private final UUID studentId = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private final UUID otherStudentId = UUID.fromString("40000000-0000-0000-0000-000000000002");
    private final UUID studentUserId = UUID.fromString("20000000-0000-0000-0000-000000000010");

    @BeforeEach
    void setUp() {
        service = new AdmitCardService(academicYearRepository, classRepository, sectionRepository, subjectRepository,
                studentRepository, enrollmentRepository, scheduleRepository, teacherRepository,
                teacherSectionRepository, schoolRepository, minioService, pdfService);
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("ADMIN"),
                Set.of("ADMIT_CARD_READ", "EXAM_MANAGE"));
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void studentCanLoadOwnPublishedCard() {
        loginStudent();
        stubRefs();
        when(studentRepository.findBySchoolIdAndUserId(TestSecurity.SCHOOL_ID, studentUserId))
                .thenReturn(Optional.of(student(studentId, studentUserId, "Asha")));
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(studentId, yearId))
                .thenReturn(Optional.of(enrollment(studentId, sectionId)));
        when(scheduleRepository.findBySchoolIdAndAcademicYearIdOrderByExamDateAscStartTimeAsc(
                TestSecurity.SCHOOL_ID, yearId)).thenReturn(List.of(publishedSchedule()));
        when(subjectRepository.findBySchoolIdOrderByNameAsc(TestSecurity.SCHOOL_ID))
                .thenReturn(List.of(subject()));
        when(schoolRepository.findById(TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(school()));

        AdmitCardDto card = service.mine(yearId, "TERM");

        assertTrue(card.published());
        assertEquals(studentId, card.studentId());
        assertEquals(1, card.datesheet().size());
        assertEquals("Mathematics", card.datesheet().get(0).subjectName());
        assertEquals("Room 101", card.datesheet().get(0).room());
    }

    @Test
    void studentCannotLoadAnotherStudentCard() {
        loginStudent();
        when(studentRepository.findBySchoolIdAndUserId(TestSecurity.SCHOOL_ID, studentUserId))
                .thenReturn(Optional.of(student(studentId, studentUserId, "Asha")));

        assertThrows(AuthException.class, () -> service.get(otherStudentId, yearId, "TERM"));
    }

    @Test
    void exportBlockedUntilPublished() {
        stubRefs();
        when(studentRepository.findBySectionAndYear(TestSecurity.SCHOOL_ID, sectionId, yearId))
                .thenReturn(List.of(student(studentId, studentUserId, "Asha")));
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(studentId, yearId))
                .thenReturn(Optional.of(enrollment(studentId, sectionId)));
        when(scheduleRepository.findBySchoolIdAndAcademicYearIdOrderByExamDateAscStartTimeAsc(
                TestSecurity.SCHOOL_ID, yearId)).thenReturn(List.of(draftSchedule()));

        AdmitCardExportRequest request = new AdmitCardExportRequest(yearId, "TERM", classId, sectionId, List.of(studentId));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.exportPdf(request));
        assertEquals("admit_card.not_published", ex.getCode());
    }

    @Test
    void rosterMarksUnpublishedUntilScheduleExists() {
        stubSection();
        when(academicYearRepository.findByIdAndSchoolId(yearId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(year()));
        when(studentRepository.findBySectionAndYear(TestSecurity.SCHOOL_ID, sectionId, yearId))
                .thenReturn(List.of(student(studentId, studentUserId, "Asha")));
        when(enrollmentRepository.findBySectionIdAndAcademicYearId(sectionId, yearId))
                .thenReturn(List.of(enrollment(studentId, sectionId)));
        when(scheduleRepository.findBySchoolIdAndAcademicYearIdOrderByExamDateAscStartTimeAsc(
                TestSecurity.SCHOOL_ID, yearId)).thenReturn(List.of());

        List<AdmitCardStudentDto> roster = service.roster(yearId, classId, sectionId, "TERM");

        assertEquals(1, roster.size());
        assertFalse(roster.get(0).published());
        assertEquals(0, roster.get(0).examCount());
    }

    @Test
    void teacherCannotAccessUnassignedSection() {
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("TEACHER"),
                Set.of("ADMIT_CARD_READ", "EXAM_READ"));
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

    private void loginStudent() {
        TestSecurity.login(studentUserId, TestSecurity.SCHOOL_ID, List.of("STUDENT"), Set.of("ADMIT_CARD_READ"));
    }

    private void stubRefs() {
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
        year.setCurrent(true);
        return year;
    }

    private SchoolClass schoolClass() {
        SchoolClass klass = new SchoolClass();
        klass.setId(classId);
        klass.setSchoolId(TestSecurity.SCHOOL_ID);
        klass.setName("Class 7");
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
        student.setAdmissionNo("ADM-" + id.toString().substring(0, 8));
        student.setFirstName(firstName);
        return student;
    }

    private StudentEnrollment enrollment(UUID student, UUID section) {
        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudentId(student);
        enrollment.setSectionId(section);
        enrollment.setAcademicYearId(yearId);
        enrollment.setSchoolId(TestSecurity.SCHOOL_ID);
        enrollment.setRollNumber(12);
        enrollment.setStatus("ACTIVE");
        return enrollment;
    }

    private ExamSchedule publishedSchedule() {
        ExamSchedule schedule = draftSchedule();
        schedule.setStatus("PUBLISHED");
        return schedule;
    }

    private ExamSchedule draftSchedule() {
        ExamSchedule schedule = new ExamSchedule();
        schedule.setId(UUID.fromString("80000000-0000-0000-0000-000000000001"));
        schedule.setSchoolId(TestSecurity.SCHOOL_ID);
        schedule.setAcademicYearId(yearId);
        schedule.setClassId(classId);
        schedule.setSectionId(sectionId);
        schedule.setSubjectId(subjectId);
        schedule.setExamTerm("TERM");
        schedule.setExamDate(LocalDate.of(2026, 9, 15));
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(11, 0));
        schedule.setRoom("Room 101");
        schedule.setStatus("DRAFT");
        return schedule;
    }

    private Subject subject() {
        Subject subject = new Subject();
        subject.setId(subjectId);
        subject.setSchoolId(TestSecurity.SCHOOL_ID);
        subject.setName("Mathematics");
        return subject;
    }

    private School school() {
        School school = new School();
        school.setId(TestSecurity.SCHOOL_ID);
        school.setName("Demo School");
        school.setAddress("1 Education Road");
        school.setPhone("0000000000");
        return school;
    }
}
