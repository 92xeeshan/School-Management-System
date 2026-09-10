package com.schoolms.academics;

import com.schoolms.TestSecurity;
import com.schoolms.academics.dto.AcademicYearRequest;
import com.schoolms.academics.dto.ClassRequest;
import com.schoolms.academics.dto.ClassUpdateRequest;
import com.schoolms.academics.dto.SubjectRequest;
import com.schoolms.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicsServiceTest {

    @Mock
    private AcademicYearRepository academicYearRepository;
    @Mock
    private SchoolClassRepository classRepository;
    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private TeacherProfileRepository teacherRepository;
    @Mock
    private TeacherSubjectRepository teacherSubjectRepository;
    @Mock
    private TeacherSectionRepository teacherSectionRepository;
    @Mock
    private ClassSubjectRepository classSubjectRepository;
    @Mock
    private StudentEnrollmentRepository enrollmentRepository;

    private AcademicsService academicsService;

    @BeforeEach
    void setUp() {
        academicsService = new AcademicsService(academicYearRepository, classRepository,
                sectionRepository, subjectRepository, teacherRepository,
                teacherSubjectRepository, teacherSectionRepository, classSubjectRepository,
                enrollmentRepository);
        TestSecurity.loginAsAdmin();
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void createClassPersistsWithSortOrder() {
        when(classRepository.saveAndFlush(any(SchoolClass.class))).thenAnswer(invocation -> {
            SchoolClass c = invocation.getArgument(0);
            if (c.getId() == null) {
                c.setId(UUID.randomUUID());
            }
            return c;
        });

        when(classRepository.findBySchoolIdAndName(TestSecurity.SCHOOL_ID, "Class 7"))
                .thenReturn(java.util.Optional.empty());
        when(sectionRepository.findBySchoolIdAndClassIdOrderByNameAsc(any(), any()))
                .thenReturn(java.util.List.of());
        when(classSubjectRepository.findByClassIdAndSchoolId(any(), any())).thenReturn(java.util.List.of());
        when(teacherRepository.findBySchoolIdOrderByFirstNameAsc(TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.List.of());
        when(teacherSectionRepository.findBySchoolId(TestSecurity.SCHOOL_ID)).thenReturn(java.util.List.of());
        when(academicYearRepository.findBySchoolIdAndCurrentTrue(TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.empty());

        var dto = academicsService.createClass(new ClassRequest("Class 7", "C7", 7, null, null, null, null, null));

        assertEquals("Class 7", dto.name());
        assertEquals(7, dto.sortOrder());
        verify(classRepository).saveAndFlush(any(SchoolClass.class));
    }

    @Test
    void createClassReturnsSectionTeacherAndSubjects() {
        UUID classId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();

        when(classRepository.findBySchoolIdAndName(TestSecurity.SCHOOL_ID, "Class 8"))
                .thenReturn(java.util.Optional.empty());
        when(classRepository.saveAndFlush(any(SchoolClass.class))).thenAnswer(invocation -> {
            SchoolClass c = invocation.getArgument(0);
            c.setId(classId);
            return c;
        });
        when(sectionRepository.existsByClassIdAndName(classId, "A")).thenReturn(false);
        when(sectionRepository.saveAndFlush(any(Section.class))).thenAnswer(invocation -> {
            Section section = invocation.getArgument(0);
            section.setId(sectionId);
            return section;
        });
        TeacherProfile teacher = new TeacherProfile();
        teacher.setId(teacherId);
        teacher.setFirstName("Asha");
        teacher.setLastName("Sharma");
        when(teacherRepository.findByIdAndSchoolId(teacherId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(teacher));
        AcademicYear year = new AcademicYear();
        year.setId(yearId);
        when(academicYearRepository.findBySchoolIdAndCurrentTrue(TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(year));
        when(teacherSectionRepository.findByTeacherIdAndSectionIdAndAcademicYearId(teacherId, sectionId, yearId))
                .thenReturn(java.util.Optional.empty());
        when(teacherSectionRepository.saveAndFlush(any(TeacherSection.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subject math = new Subject();
        math.setId(subjectId);
        math.setName("Mathematics");
        math.setCode("MATH");
        when(subjectRepository.findByIdAndSchoolId(subjectId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(math));
        when(classSubjectRepository.existsByClassIdAndSubjectId(classId, subjectId)).thenReturn(false);
        when(classSubjectRepository.saveAndFlush(any(ClassSubject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(teacherRepository.findBySchoolIdOrderByFirstNameAsc(TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.List.of(teacher));
        TeacherSection assignment = new TeacherSection();
        assignment.setSectionId(sectionId);
        assignment.setTeacherId(teacherId);
        assignment.setClassTeacher(true);
        when(teacherSectionRepository.findBySchoolId(TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.List.of())
                .thenReturn(java.util.List.of(assignment));
        Section savedSection = new Section();
        savedSection.setId(sectionId);
        savedSection.setClassId(classId);
        savedSection.setName("A");
        savedSection.setCapacity(40);
        savedSection.setRoom("R-12");
        when(sectionRepository.findBySchoolIdAndClassIdOrderByNameAsc(TestSecurity.SCHOOL_ID, classId))
                .thenReturn(java.util.List.of(savedSection));
        when(subjectRepository.findBySchoolIdOrderByNameAsc(TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.List.of(math));
        ClassSubject link = new ClassSubject();
        link.setClassId(classId);
        link.setSubjectId(subjectId);
        when(classSubjectRepository.findByClassIdAndSchoolId(classId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.List.of(link));

        when(enrollmentRepository.findBySchoolIdAndAcademicYearIdAndStatus(
                TestSecurity.SCHOOL_ID, yearId, "ACTIVE")).thenReturn(java.util.List.of());

        var dto = academicsService.createClass(new ClassRequest(
                "Class 8", "C8", 8, "A", 40, "R-12", java.util.List.of(subjectId), teacherId));

        assertEquals("Class 8", dto.name());
        assertEquals(1, dto.sections().size());
        assertEquals("A", dto.sections().get(0).name());
        assertEquals("Asha Sharma", dto.sections().get(0).classTeacherName());
        assertEquals("R-12", dto.sections().get(0).room());
        assertEquals(0, dto.sections().get(0).studentCount());
        assertEquals(1, dto.subjects().size());
        assertEquals("Mathematics", dto.subjects().get(0).name());
    }

    @Test
    void createClassRejectsDuplicateSection() {
        SchoolClass existing = new SchoolClass();
        existing.setId(UUID.randomUUID());
        existing.setName("Class 5");
        when(classRepository.findBySchoolIdAndName(TestSecurity.SCHOOL_ID, "Class 5"))
                .thenReturn(java.util.Optional.of(existing));
        when(sectionRepository.existsByClassIdAndName(existing.getId(), "A")).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> academicsService.createClass(new ClassRequest("Class 5", "C5", 5, "A", 40, null, null, null)));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void createYearClearsPreviousCurrentFlag() {
        UUID yearId = UUID.randomUUID();
        AcademicYear previous = new AcademicYear();
        previous.setId(yearId);
        previous.setCurrent(true);
        when(academicYearRepository.existsBySchoolIdAndName(TestSecurity.SCHOOL_ID, "2026-27"))
                .thenReturn(false);
        when(academicYearRepository.findBySchoolIdAndCurrentTrue(TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(previous));
        when(academicYearRepository.save(any(AcademicYear.class))).thenAnswer(invocation -> {
            AcademicYear y = invocation.getArgument(0);
            if (y.getId() == null) {
                y.setId(UUID.randomUUID());
            }
            return y;
        });

        AcademicYearRequest request = new AcademicYearRequest("2026-27",
                LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31), true);

        var dto = academicsService.createYear(request);

        assertTrue(!previous.isCurrent(), "previous current year should be cleared");
        assertEquals("2026-27", dto.name());
        verify(academicYearRepository).save(previous);
    }

    @Test
    void updateClassRejectsDuplicateName() {
        UUID classId = UUID.randomUUID();
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(classId);
        schoolClass.setName("Class 8");
        when(classRepository.findByIdAndSchoolId(classId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(schoolClass));
        when(classRepository.existsBySchoolIdAndNameAndIdNot(TestSecurity.SCHOOL_ID, "Class 9", classId))
                .thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> academicsService.updateClass(classId, new ClassUpdateRequest(
                        "Class 9", "C9", null, null, null, null, null, null)));
        assertEquals("class.name_exists", ex.getCode());
    }

    @Test
    void updateClassRejectsCapacityBelowEnrollment() {
        UUID classId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(classId);
        schoolClass.setName("Class 8");
        Section section = new Section();
        section.setId(sectionId);
        section.setClassId(classId);
        section.setName("A");
        section.setCapacity(40);
        when(classRepository.findByIdAndSchoolId(classId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(schoolClass));
        when(classRepository.existsBySchoolIdAndNameAndIdNot(TestSecurity.SCHOOL_ID, "Class 8", classId))
                .thenReturn(false);
        when(classRepository.saveAndFlush(schoolClass)).thenReturn(schoolClass);
        when(sectionRepository.findByIdAndSchoolId(sectionId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(section));
        when(sectionRepository.existsByClassIdAndNameAndIdNot(classId, "A", sectionId)).thenReturn(false);
        AcademicYear year = new AcademicYear();
        year.setId(yearId);
        when(academicYearRepository.findBySchoolIdAndCurrentTrue(TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(year));
        when(enrollmentRepository.countBySectionIdAndAcademicYearIdAndStatus(sectionId, yearId, "ACTIVE"))
                .thenReturn(12L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> academicsService.updateClass(classId, new ClassUpdateRequest(
                        "Class 8", "C8", sectionId, "A", 10, "R-1", null, null)));
        assertEquals("section.capacity_below_enrollment", ex.getCode());
    }

    @Test
    void updateClassRejectsTeacherConflict() {
        UUID classId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID otherSectionId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(classId);
        schoolClass.setName("Class 8");
        Section section = new Section();
        section.setId(sectionId);
        section.setClassId(classId);
        section.setName("A");
        section.setCapacity(40);
        when(classRepository.findByIdAndSchoolId(classId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(schoolClass));
        when(classRepository.existsBySchoolIdAndNameAndIdNot(TestSecurity.SCHOOL_ID, "Class 8", classId))
                .thenReturn(false);
        when(classRepository.saveAndFlush(schoolClass)).thenReturn(schoolClass);
        when(sectionRepository.findByIdAndSchoolId(sectionId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(section));
        when(sectionRepository.existsByClassIdAndNameAndIdNot(classId, "A", sectionId)).thenReturn(false);
        when(sectionRepository.saveAndFlush(section)).thenReturn(section);
        AcademicYear year = new AcademicYear();
        year.setId(yearId);
        when(academicYearRepository.findBySchoolIdAndCurrentTrue(TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(year));
        when(enrollmentRepository.countBySectionIdAndAcademicYearIdAndStatus(sectionId, yearId, "ACTIVE"))
                .thenReturn(0L);
        TeacherSection other = new TeacherSection();
        other.setTeacherId(teacherId);
        other.setSectionId(otherSectionId);
        other.setAcademicYearId(yearId);
        other.setClassTeacher(true);
        when(teacherSectionRepository.findBySchoolId(TestSecurity.SCHOOL_ID)).thenReturn(java.util.List.of(other));
        TeacherProfile teacher = new TeacherProfile();
        teacher.setId(teacherId);
        teacher.setFirstName("Asha");
        when(teacherRepository.findByIdAndSchoolId(teacherId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(teacher));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> academicsService.updateClass(classId, new ClassUpdateRequest(
                        "Class 8", "C8", sectionId, "A", 40, "R-1", null, teacherId)));
        assertEquals("section.teacher_conflict", ex.getCode());
    }

    @Test
    void updateClassSavesRoomTeacherAndSubjects() {
        UUID classId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(classId);
        schoolClass.setSchoolId(TestSecurity.SCHOOL_ID);
        schoolClass.setName("Class 8");
        schoolClass.setCode("C8");
        Section section = new Section();
        section.setId(sectionId);
        section.setClassId(classId);
        section.setName("A");
        section.setCapacity(40);
        when(classRepository.findByIdAndSchoolId(classId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(schoolClass));
        when(classRepository.existsBySchoolIdAndNameAndIdNot(TestSecurity.SCHOOL_ID, "Class 8A", classId))
                .thenReturn(false);
        when(classRepository.saveAndFlush(any(SchoolClass.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sectionRepository.findByIdAndSchoolId(sectionId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(section));
        when(sectionRepository.existsByClassIdAndNameAndIdNot(classId, "B", sectionId)).thenReturn(false);
        when(sectionRepository.saveAndFlush(any(Section.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AcademicYear year = new AcademicYear();
        year.setId(yearId);
        when(academicYearRepository.findBySchoolIdAndCurrentTrue(TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(year));
        when(enrollmentRepository.countBySectionIdAndAcademicYearIdAndStatus(sectionId, yearId, "ACTIVE"))
                .thenReturn(8L);
        when(teacherSectionRepository.findBySchoolId(TestSecurity.SCHOOL_ID)).thenReturn(java.util.List.of());
        TeacherProfile teacher = new TeacherProfile();
        teacher.setId(teacherId);
        teacher.setFirstName("Asha");
        teacher.setLastName("Sharma");
        when(teacherRepository.findByIdAndSchoolId(teacherId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(teacher));
        when(teacherSectionRepository.findByTeacherIdAndSectionIdAndAcademicYearId(teacherId, sectionId, yearId))
                .thenReturn(java.util.Optional.empty());
        when(teacherSectionRepository.saveAndFlush(any(TeacherSection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Subject math = new Subject();
        math.setId(subjectId);
        math.setName("Mathematics");
        math.setCode("MATH");
        when(subjectRepository.findByIdAndSchoolId(subjectId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(math));
        ClassSubject savedLink = new ClassSubject();
        savedLink.setClassId(classId);
        savedLink.setSubjectId(subjectId);
        when(classSubjectRepository.findByClassIdAndSchoolId(classId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.List.of())
                .thenReturn(java.util.List.of(savedLink));
        when(classSubjectRepository.saveAndFlush(any(ClassSubject.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(teacherRepository.findBySchoolIdOrderByFirstNameAsc(TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.List.of(teacher));
        TeacherSection assignment = new TeacherSection();
        assignment.setSectionId(sectionId);
        assignment.setTeacherId(teacherId);
        assignment.setClassTeacher(true);
        when(teacherSectionRepository.findBySchoolId(TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.List.of())
                .thenReturn(java.util.List.of(assignment));
        when(sectionRepository.findBySchoolIdAndClassIdOrderByNameAsc(TestSecurity.SCHOOL_ID, classId))
                .thenReturn(java.util.List.of(section));
        when(subjectRepository.findBySchoolIdOrderByNameAsc(TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.List.of(math));
        when(enrollmentRepository.findBySchoolIdAndAcademicYearIdAndStatus(
                TestSecurity.SCHOOL_ID, yearId, "ACTIVE")).thenReturn(java.util.List.of());

        var dto = academicsService.updateClass(classId, new ClassUpdateRequest(
                "Class 8A", "C8A", sectionId, "B", 35, "Lab-2", java.util.List.of(subjectId), teacherId));

        assertEquals("Class 8A", dto.name());
        assertEquals("B", dto.sections().get(0).name());
        assertEquals(35, dto.sections().get(0).capacity());
        assertEquals("Lab-2", dto.sections().get(0).room());
        assertEquals("Asha Sharma", dto.sections().get(0).classTeacherName());
        assertEquals("Mathematics", dto.subjects().get(0).name());
    }

    @Test
    void deleteClassRejectsWhenStudentsEnrolled() {
        UUID classId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(classId);
        Section section = new Section();
        section.setId(sectionId);
        section.setClassId(classId);
        when(classRepository.findByIdAndSchoolId(classId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(schoolClass));
        when(sectionRepository.findBySchoolIdAndClassIdOrderByNameAsc(TestSecurity.SCHOOL_ID, classId))
                .thenReturn(java.util.List.of(section));
        AcademicYear year = new AcademicYear();
        year.setId(yearId);
        when(academicYearRepository.findBySchoolIdAndCurrentTrue(TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(year));
        when(enrollmentRepository.countBySectionIdAndAcademicYearIdAndStatus(sectionId, yearId, "ACTIVE"))
                .thenReturn(3L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> academicsService.deleteClass(classId));
        assertEquals("class.has_students", ex.getCode());
        verify(classRepository, never()).delete(any());
    }

    @Test
    void createSubjectPersistsMetadataAndMappings() {
        UUID subjectId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        when(subjectRepository.existsBySchoolIdAndName(TestSecurity.SCHOOL_ID, "Physics"))
                .thenReturn(false);
        when(subjectRepository.saveAndFlush(any(Subject.class))).thenAnswer(invocation -> {
            Subject subject = invocation.getArgument(0);
            subject.setId(subjectId);
            return subject;
        });
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(classId);
        schoolClass.setName("Class 8");
        schoolClass.setCode("C8");
        when(classRepository.findByIdAndSchoolId(classId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(schoolClass));
        TeacherProfile teacher = new TeacherProfile();
        teacher.setId(teacherId);
        teacher.setFirstName("Asha");
        teacher.setLastName("Sharma");
        when(teacherRepository.findByIdAndSchoolId(teacherId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(teacher));
        when(classSubjectRepository.findBySubjectIdAndSchoolId(subjectId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.List.of())
                .thenReturn(java.util.List.of(classSubject(classId, subjectId, teacherId)));
        when(classSubjectRepository.saveAndFlush(any(ClassSubject.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(teacherSubjectRepository.findBySubjectIdAndSchoolId(subjectId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.List.of());
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectId(teacherId, subjectId)).thenReturn(false);
        when(teacherSubjectRepository.saveAndFlush(any(TeacherSubject.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(classRepository.findBySchoolIdOrderBySortOrderAsc(TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.List.of(schoolClass));
        when(teacherRepository.findBySchoolIdOrderByFirstNameAsc(TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.List.of(teacher));

        var dto = academicsService.createSubject(new SubjectRequest(
                "Physics", "PHY", "ELECTIVE", "Lab science", 4, true, "ACTIVE",
                java.util.List.of(classId), teacherId));

        assertEquals("Physics", dto.name());
        assertEquals("PHY", dto.code());
        assertEquals("ELECTIVE", dto.type().name());
        assertEquals(4, dto.weeklyPeriods());
        assertTrue(dto.practical());
        assertEquals("ACTIVE", dto.status());
        assertEquals("Asha Sharma", dto.teacherName());
        assertEquals(1, dto.classes().size());
        assertEquals("Class 8", dto.classes().get(0).name());
        verify(classSubjectRepository).saveAndFlush(any(ClassSubject.class));
        verify(teacherSubjectRepository).saveAndFlush(any(TeacherSubject.class));
    }

    @Test
    void createSubjectRejectsDuplicateName() {
        when(subjectRepository.existsBySchoolIdAndName(TestSecurity.SCHOOL_ID, "Mathematics"))
                .thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> academicsService.createSubject(new SubjectRequest(
                        "Mathematics", "MATH", "CORE", null, 5, false, "ACTIVE", null, null)));
        assertEquals("subject.name_exists", ex.getCode());
        verify(subjectRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateSubjectReplacesClassesAndTeacher() {
        UUID subjectId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        Subject subject = new Subject();
        subject.setId(subjectId);
        subject.setSchoolId(TestSecurity.SCHOOL_ID);
        subject.setName("Science");
        subject.setCode("SCI");
        when(subjectRepository.findByIdAndSchoolId(subjectId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(subject));
        when(subjectRepository.existsBySchoolIdAndNameAndIdNot(TestSecurity.SCHOOL_ID, "Science Lab", subjectId))
                .thenReturn(false);
        when(subjectRepository.saveAndFlush(any(Subject.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(classId);
        schoolClass.setName("Class 9");
        schoolClass.setCode("C9");
        when(classRepository.findByIdAndSchoolId(classId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(schoolClass));
        TeacherProfile teacher = new TeacherProfile();
        teacher.setId(teacherId);
        teacher.setFirstName("Ravi");
        teacher.setLastName("Mehta");
        when(teacherRepository.findByIdAndSchoolId(teacherId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(teacher));
        when(classSubjectRepository.findBySubjectIdAndSchoolId(subjectId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.List.of())
                .thenReturn(java.util.List.of(classSubject(classId, subjectId, teacherId)));
        when(classSubjectRepository.saveAndFlush(any(ClassSubject.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectId(teacherId, subjectId)).thenReturn(false);
        when(teacherSubjectRepository.saveAndFlush(any(TeacherSubject.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(teacherSubjectRepository.findBySubjectIdAndSchoolId(subjectId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.List.of());
        when(classRepository.findBySchoolIdOrderBySortOrderAsc(TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.List.of(schoolClass));
        when(teacherRepository.findBySchoolIdOrderByFirstNameAsc(TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.List.of(teacher));

        var dto = academicsService.updateSubject(subjectId, new SubjectRequest(
                "Science Lab", "SCI", "CORE", null, 5, true, "INACTIVE",
                java.util.List.of(classId), teacherId));

        assertEquals("Science Lab", dto.name());
        assertTrue(dto.practical());
        assertEquals("INACTIVE", dto.status());
        assertEquals(teacherId, dto.teacherId());
        assertEquals(1, dto.classes().size());
        verify(teacherSubjectRepository).deleteBySubjectIdAndSchoolId(subjectId, TestSecurity.SCHOOL_ID);
    }

    private ClassSubject classSubject(UUID classId, UUID subjectId, UUID teacherId) {
        ClassSubject link = new ClassSubject();
        link.setClassId(classId);
        link.setSubjectId(subjectId);
        link.setTeacherId(teacherId);
        return link;
    }
}
