package com.schoolms.syllabus;

import com.schoolms.TestSecurity;
import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.SchoolClass;
import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.academics.Section;
import com.schoolms.academics.SectionRepository;
import com.schoolms.academics.Subject;
import com.schoolms.academics.SubjectRepository;
import com.schoolms.academics.TeacherProfile;
import com.schoolms.academics.TeacherProfileRepository;
import com.schoolms.common.enums.SyllabusChapterStatus;
import com.schoolms.common.enums.SyllabusMaterialKind;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.syllabus.dto.SyllabusChapterRequest;
import com.schoolms.syllabus.dto.SyllabusChapterStatusRequest;
import com.schoolms.syllabus.dto.SyllabusCourseDto;
import com.schoolms.syllabus.dto.SyllabusCourseRequest;
import com.schoolms.syllabus.dto.SyllabusMaterialDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyllabusServiceTest {

    @Mock
    private SyllabusCourseRepository courseRepository;
    @Mock
    private SyllabusUnitRepository unitRepository;
    @Mock
    private SyllabusChapterRepository chapterRepository;
    @Mock
    private SyllabusMaterialRepository materialRepository;
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

    private SyllabusService service;

    private final UUID yearId = UUID.randomUUID();
    private final UUID classId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SyllabusService(courseRepository, unitRepository, chapterRepository, materialRepository,
                academicYearRepository, classRepository, sectionRepository, subjectRepository, teacherRepository);
        TestSecurity.loginAsAdmin();
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void listComputesChapterAndUnitProgress() {
        SyllabusCourse course = course("Maths", LocalDate.now().plusDays(30));
        when(courseRepository.findBySchoolIdOrderByCreatedAtDesc(TestSecurity.SCHOOL_ID))
                .thenReturn(List.of(course));
        SyllabusUnit unit1 = unit(course.getId(), "Unit 1", 1);
        SyllabusUnit unit2 = unit(course.getId(), "Unit 2", 2);
        when(unitRepository.findBySchoolIdAndCourseIdInOrderBySortOrderAscCreatedAtAsc(
                TestSecurity.SCHOOL_ID, List.of(course.getId())))
                .thenReturn(List.of(unit1, unit2));
        when(chapterRepository.findBySchoolIdAndUnitIdInOrderBySortOrderAscCreatedAtAsc(
                TestSecurity.SCHOOL_ID, List.of(unit1.getId(), unit2.getId())))
                .thenReturn(List.of(
                        chapter(unit1.getId(), "A", SyllabusChapterStatus.COMPLETED, 1),
                        chapter(unit1.getId(), "B", SyllabusChapterStatus.COMPLETED, 2),
                        chapter(unit2.getId(), "C", SyllabusChapterStatus.IN_PROGRESS, 1),
                        chapter(unit2.getId(), "D", SyllabusChapterStatus.PENDING, 2)));

        List<SyllabusCourseDto> result = service.list(yearId, classId, sectionId, subjectId, null, null, null);

        assertEquals(1, result.size());
        SyllabusCourseDto dto = result.get(0);
        assertEquals(2, dto.totalUnits());
        assertEquals(1, dto.completedUnits());
        assertEquals(4, dto.totalChapters());
        assertEquals(2, dto.completedChapters());
        assertEquals(1, dto.inProgressChapters());
        assertEquals(2, dto.remainingChapters());
        assertEquals(50, dto.completionPercent());
        assertEquals("IN_PROGRESS", dto.progressStatus());
    }

    @Test
    void listFiltersByProgressStatus() {
        SyllabusCourse completedCourse = course("Maths", null);
        SyllabusCourse pendingCourse = course("Science", null);
        when(courseRepository.findBySchoolIdOrderByCreatedAtDesc(TestSecurity.SCHOOL_ID))
                .thenReturn(List.of(completedCourse, pendingCourse));
        SyllabusUnit doneUnit = unit(completedCourse.getId(), "Unit 1", 1);
        SyllabusUnit openUnit = unit(pendingCourse.getId(), "Unit 1", 1);
        when(unitRepository.findBySchoolIdAndCourseIdInOrderBySortOrderAscCreatedAtAsc(
                TestSecurity.SCHOOL_ID, List.of(completedCourse.getId(), pendingCourse.getId())))
                .thenReturn(List.of(doneUnit, openUnit));
        when(chapterRepository.findBySchoolIdAndUnitIdInOrderBySortOrderAscCreatedAtAsc(
                TestSecurity.SCHOOL_ID, List.of(doneUnit.getId(), openUnit.getId())))
                .thenReturn(List.of(
                        chapter(doneUnit.getId(), "A", SyllabusChapterStatus.COMPLETED, 1),
                        chapter(openUnit.getId(), "B", SyllabusChapterStatus.PENDING, 1)));

        List<SyllabusCourseDto> result = service.list(yearId, classId, sectionId, subjectId, null, "COMPLETED", null);

        assertEquals(1, result.size());
        assertEquals(completedCourse.getId(), result.get(0).id());
    }

    @Test
    void listMatchesSearchAcrossSubjectAndTeacher() {
        SyllabusCourse course = course("Algebra", null);
        when(courseRepository.findBySchoolIdOrderByCreatedAtDesc(TestSecurity.SCHOOL_ID))
                .thenReturn(List.of(course));

        assertEquals(1, service.list(null, null, null, null, null, null, "algebra").size());
        assertEquals(0, service.list(null, null, null, null, null, null, "physics").size());
    }

    @Test
    void createRejectsDuplicateSectionCourse() {
        stubValidRefs();
        SyllabusCourse existing = course("Maths", null);
        existing.setSectionId(sectionId);
        when(courseRepository.findBySchoolIdAndAcademicYearIdAndClassIdAndSubjectId(
                TestSecurity.SCHOOL_ID, yearId, classId, subjectId)).thenReturn(List.of(existing));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(request(sectionId)));
        assertEquals("syllabus.course_exists", ex.getCode());
        verify(courseRepository, never()).save(any());
    }

    @Test
    void createAllowsSameSubjectInDifferentSection() {
        stubValidRefs();
        SyllabusCourse existing = course("Maths", null);
        existing.setSectionId(UUID.randomUUID());
        when(courseRepository.findBySchoolIdAndAcademicYearIdAndClassIdAndSubjectId(
                TestSecurity.SCHOOL_ID, yearId, classId, subjectId)).thenReturn(List.of(existing));
        when(courseRepository.save(any(SyllabusCourse.class))).thenAnswer(inv -> inv.getArgument(0));

        SyllabusCourseDto dto = service.create(request(sectionId));

        assertEquals("Maths", dto.title());
        assertEquals("ACTIVE", dto.status());
        verify(courseRepository).save(any(SyllabusCourse.class));
    }

    @Test
    void createRejectsSectionFromAnotherClass() {
        AcademicYear year = new AcademicYear();
        year.setName("2025-26");
        when(academicYearRepository.findByIdAndSchoolId(yearId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(year));
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName("Class 5");
        when(classRepository.findByIdAndSchoolId(classId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(schoolClass));
        Subject subject = new Subject();
        subject.setName("Maths");
        when(subjectRepository.findByIdAndSchoolId(subjectId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(subject));
        Section section = new Section();
        section.setClassId(UUID.randomUUID());
        when(sectionRepository.findByIdAndSchoolId(sectionId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(section));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(request(sectionId)));
        assertEquals("syllabus.section_class_mismatch", ex.getCode());
    }

    @Test
    void markingChapterCompletedStampsAuditFields() {
        SyllabusChapter chapter = chapter(UUID.randomUUID(), "Place value", SyllabusChapterStatus.PENDING, 1);
        when(chapterRepository.findByIdAndSchoolId(chapter.getId(), TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(chapter));
        when(chapterRepository.save(chapter)).thenReturn(chapter);

        service.updateChapterStatus(chapter.getId(),
                new SyllabusChapterStatusRequest(SyllabusChapterStatus.COMPLETED));

        assertEquals(SyllabusChapterStatus.COMPLETED, chapter.getStatus());
        assertNotNull(chapter.getCompletedAt());
        assertEquals(TestSecurity.USER_ID, chapter.getCompletedByUserId());
    }

    @Test
    void reopeningChapterClearsCompletion() {
        SyllabusChapter chapter = chapter(UUID.randomUUID(), "Fractions", SyllabusChapterStatus.COMPLETED, 1);
        chapter.setCompletedAt(java.time.Instant.now());
        chapter.setCompletedByUserId(TestSecurity.USER_ID);
        when(chapterRepository.findByIdAndSchoolId(chapter.getId(), TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(chapter));
        when(chapterRepository.save(chapter)).thenReturn(chapter);

        service.updateChapterStatus(chapter.getId(),
                new SyllabusChapterStatusRequest(SyllabusChapterStatus.PENDING));

        assertEquals(SyllabusChapterStatus.PENDING, chapter.getStatus());
        assertNull(chapter.getCompletedAt());
        assertNull(chapter.getCompletedByUserId());
    }

    @Test
    void creatingChapterWithCompletedStatusStampsCompletion() {
        SyllabusUnit unit = unit(UUID.randomUUID(), "Unit", 1);
        when(unitRepository.findByIdAndSchoolId(unit.getId(), TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(unit));
        when(chapterRepository.save(any(SyllabusChapter.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.createChapter(unit.getId(), new SyllabusChapterRequest(
                "Chapter one", null, 1, SyllabusChapterStatus.COMPLETED, null));

        assertEquals("COMPLETED", dto.status());
        assertNotNull(dto.completedAt());
    }

    @Test
    void materialRequiresFileOrLink() {
        SyllabusCourse course = course("Maths", null);
        when(courseRepository.findByIdAndSchoolId(course.getId(), TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(course));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.addMaterial(course.getId(), SyllabusMaterialKind.DOCUMENT,
                        "Outline", null, null, null));
        assertEquals("syllabus.material_source_required", ex.getCode());
    }

    @Test
    void linkMaterialRequiresUrl() {
        SyllabusCourse course = course("Maths", null);
        when(courseRepository.findByIdAndSchoolId(course.getId(), TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(course));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.addMaterial(course.getId(), SyllabusMaterialKind.LINK,
                        "Portal", null, "  ", null));
        assertEquals("syllabus.material_url_required", ex.getCode());
    }

    @Test
    void materialRejectsOversizedFile() {
        SyllabusCourse course = course("Maths", null);
        when(courseRepository.findByIdAndSchoolId(course.getId(), TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(course));
        MockMultipartFile file = new MockMultipartFile("file", "big.pdf", "application/pdf",
                new byte[(int) SyllabusService.MAX_MATERIAL_BYTES + 1]);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.addMaterial(course.getId(), SyllabusMaterialKind.DOCUMENT,
                        "Big", null, null, file));
        assertEquals("syllabus.material_too_large", ex.getCode());
    }

    @Test
    void uploadingMaterialStoresBytesAndMetadata() {
        SyllabusCourse course = course("Maths", null);
        when(courseRepository.findByIdAndSchoolId(course.getId(), TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(course));
        when(materialRepository.save(any(SyllabusMaterial.class))).thenAnswer(inv -> inv.getArgument(0));
        byte[] bytes = "outline".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "outline.txt", "text/plain", bytes);

        SyllabusMaterialDto dto = service.addMaterial(course.getId(), SyllabusMaterialKind.OUTLINE,
                "Course outline", null, null, file);

        assertEquals("Course outline", dto.title());
        assertEquals("outline.txt", dto.fileName());
        assertTrue(dto.downloadable());
        assertEquals((long) bytes.length, dto.fileSize());

        when(materialRepository.findByIdAndSchoolId(any(), any())).thenAnswer(inv -> {
            SyllabusMaterial material = new SyllabusMaterial();
            material.setTitle("Course outline");
            material.setFileName("outline.txt");
            material.setContentType("text/plain");
            material.setContent(bytes);
            return Optional.of(material);
        });
        assertArrayEquals(bytes, service.download(UUID.randomUUID()).content());
    }

    @Test
    void materialForAnotherCourseUnitIsRejected() {
        SyllabusCourse course = course("Maths", null);
        when(courseRepository.findByIdAndSchoolId(course.getId(), TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(course));
        SyllabusUnit foreign = unit(UUID.randomUUID(), "Foreign", 1);
        when(unitRepository.findByIdAndSchoolId(foreign.getId(), TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(foreign));
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", new byte[]{1});

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.addMaterial(course.getId(), SyllabusMaterialKind.DOCUMENT,
                        "Doc", foreign.getId(), null, file));
        assertEquals("syllabus.unit_not_in_course", ex.getCode());
    }

    private void stubValidRefs() {
        AcademicYear year = new AcademicYear();
        year.setName("2025-26");
        when(academicYearRepository.findByIdAndSchoolId(yearId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(year));
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName("Class 5");
        when(classRepository.findByIdAndSchoolId(classId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(schoolClass));
        Subject subject = new Subject();
        subject.setName("Maths");
        when(subjectRepository.findByIdAndSchoolId(subjectId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(subject));
        Section section = new Section();
        section.setClassId(classId);
        section.setName("A");
        when(sectionRepository.findByIdAndSchoolId(sectionId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(section));
    }

    private SyllabusCourseRequest request(UUID courseSectionId) {
        return new SyllabusCourseRequest(yearId, classId, courseSectionId, subjectId, null,
                "Maths", "Numbers and shapes", LocalDate.now().plusDays(60), null);
    }

    private SyllabusCourse course(String title, LocalDate target) {
        SyllabusCourse course = new SyllabusCourse();
        course.setId(UUID.randomUUID());
        course.setSchoolId(TestSecurity.SCHOOL_ID);
        course.setAcademicYearId(yearId);
        course.setClassId(classId);
        course.setSectionId(sectionId);
        course.setSubjectId(subjectId);
        course.setTitle(title);
        course.setTargetCompletionDate(target);
        return course;
    }

    private SyllabusUnit unit(UUID courseId, String title, int order) {
        SyllabusUnit unit = new SyllabusUnit();
        unit.setId(UUID.randomUUID());
        unit.setSchoolId(TestSecurity.SCHOOL_ID);
        unit.setCourseId(courseId);
        unit.setTitle(title);
        unit.setSortOrder(order);
        return unit;
    }

    private SyllabusChapter chapter(UUID unitId, String title, SyllabusChapterStatus status, int order) {
        SyllabusChapter chapter = new SyllabusChapter();
        chapter.setId(UUID.randomUUID());
        chapter.setSchoolId(TestSecurity.SCHOOL_ID);
        chapter.setUnitId(unitId);
        chapter.setTitle(title);
        chapter.setStatus(status);
        chapter.setSortOrder(order);
        return chapter;
    }
}
