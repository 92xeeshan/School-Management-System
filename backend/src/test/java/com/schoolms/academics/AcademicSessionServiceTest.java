package com.schoolms.academics;

import com.schoolms.TestSecurity;
import com.schoolms.academics.dto.AcademicTermRequest;
import com.schoolms.academics.dto.AcademicYearRequest;
import com.schoolms.academics.dto.PromotionPreviewRequest;
import com.schoolms.common.enums.AcademicTermStatus;
import com.schoolms.common.enums.PromotionOutcome;
import com.schoolms.common.enums.StudentStatus;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.student.Student;
import com.schoolms.student.StudentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicSessionServiceTest {

    @Mock
    private AcademicYearRepository academicYearRepository;
    @Mock
    private AcademicTermRepository termRepository;
    @Mock
    private SchoolClassRepository classRepository;
    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private StudentEnrollmentRepository enrollmentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private PromotionBatchRepository promotionBatchRepository;
    @Mock
    private PromotionBatchItemRepository promotionItemRepository;

    private AcademicSessionService service;

    private final UUID yearId = UUID.randomUUID();
    private final UUID nextYearId = UUID.randomUUID();
    private final UUID classId = UUID.randomUUID();
    private final UUID nextClassId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();
    private final UUID nextSectionId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AcademicSessionService(academicYearRepository, termRepository, classRepository,
                sectionRepository, enrollmentRepository, studentRepository, promotionBatchRepository,
                promotionItemRepository);
        TestSecurity.loginAsAdmin();
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void createTermRejectsOverlap() {
        AcademicYear year = year(yearId, "2025-26", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31), true);
        AcademicTerm existing = term(UUID.randomUUID(), yearId, "Term 1",
                LocalDate.of(2025, 4, 1), LocalDate.of(2025, 9, 30));
        when(academicYearRepository.findByIdAndSchoolId(yearId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(year));
        when(termRepository.existsBySchoolIdAndAcademicYearIdAndName(TestSecurity.SCHOOL_ID, yearId, "Term 2"))
                .thenReturn(false);
        when(termRepository.findBySchoolIdAndAcademicYearIdOrderBySortOrderAscStartDateAsc(
                TestSecurity.SCHOOL_ID, yearId)).thenReturn(List.of(existing));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createTerm(yearId,
                new AcademicTermRequest("Term 2", LocalDate.of(2025, 9, 1), LocalDate.of(2026, 3, 31),
                        AcademicTermStatus.PLANNED, 2)));
        assertEquals("academic_term.overlap", ex.getCode());
        verify(termRepository, never()).save(any());
    }

    @Test
    void createTermRejectsDatesOutsideYear() {
        AcademicYear year = year(yearId, "2025-26", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31), true);
        when(academicYearRepository.findByIdAndSchoolId(yearId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(year));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createTerm(yearId,
                new AcademicTermRequest("Term 1", LocalDate.of(2025, 3, 1), LocalDate.of(2025, 9, 30),
                        AcademicTermStatus.PLANNED, 1)));
        assertEquals("academic_term.outside_year", ex.getCode());
    }

    @Test
    void updateYearRejectsWhenTermsWouldFallOutside() {
        AcademicYear year = year(yearId, "2025-26", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31), true);
        AcademicTerm existing = term(UUID.randomUUID(), yearId, "Term 1",
                LocalDate.of(2025, 4, 1), LocalDate.of(2025, 9, 30));
        when(academicYearRepository.findByIdAndSchoolId(yearId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(year));
        when(academicYearRepository.existsBySchoolIdAndNameAndIdNot(TestSecurity.SCHOOL_ID, "2025-26", yearId))
                .thenReturn(false);
        when(termRepository.findBySchoolIdAndAcademicYearIdOrderBySortOrderAscStartDateAsc(
                TestSecurity.SCHOOL_ID, yearId)).thenReturn(List.of(existing));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.updateYear(yearId,
                new AcademicYearRequest("2025-26", LocalDate.of(2025, 6, 1), LocalDate.of(2026, 3, 31), true)));
        assertEquals("academic_year.terms_outside_dates", ex.getCode());
    }

    @Test
    void activateYearClearsPreviousCurrent() {
        AcademicYear previous = year(UUID.randomUUID(), "2024-25", LocalDate.of(2024, 4, 1),
                LocalDate.of(2025, 3, 31), true);
        AcademicYear next = year(yearId, "2025-26", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31), false);
        when(academicYearRepository.findByIdAndSchoolId(yearId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(next));
        when(academicYearRepository.findBySchoolIdAndCurrentTrue(TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(previous));
        when(academicYearRepository.save(any(AcademicYear.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.activateYear(yearId);

        assertTrue(dto.current());
        assertFalse(previous.isCurrent());
        verify(academicYearRepository).save(previous);
        verify(academicYearRepository).save(next);
    }

    @Test
    void previewPromotionSkipsAlreadyEnrolledAndFullSection() {
        AcademicYear source = year(yearId, "2025-26", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31), true);
        AcademicYear dest = year(nextYearId, "2026-27", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31), false);
        SchoolClass sourceClass = schoolClass(classId, "Class 5");
        SchoolClass destClass = schoolClass(nextClassId, "Class 6");
        Section sourceSection = section(sectionId, classId, "A", 40);
        Section destSection = section(nextSectionId, nextClassId, "A", 1);
        Student student = student(studentId, "S-001", "Ali", "Khan");
        StudentEnrollment enrollment = enrollment(studentId, sectionId, yearId, 1);

        UUID extraStudentId = UUID.randomUUID();
        Student extra = student(extraStudentId, "S-002", "Sara", "Ahmed");
        StudentEnrollment extraEnrollment = enrollment(extraStudentId, sectionId, yearId, 2);

        when(academicYearRepository.findByIdAndSchoolId(yearId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(source));
        when(academicYearRepository.findByIdAndSchoolId(nextYearId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(dest));
        when(classRepository.findByIdAndSchoolId(classId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(sourceClass));
        when(classRepository.findByIdAndSchoolId(nextClassId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(destClass));
        when(sectionRepository.findByIdAndSchoolId(sectionId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(sourceSection));
        when(sectionRepository.findByIdAndSchoolId(nextSectionId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(destSection));
        when(enrollmentRepository.findBySchoolIdAndAcademicYearIdAndStatusAndSectionId(
                TestSecurity.SCHOOL_ID, yearId, "ACTIVE", sectionId))
                .thenReturn(List.of(enrollment, extraEnrollment));
        when(studentRepository.findBySchoolIdAndIdIn(eq(TestSecurity.SCHOOL_ID), anyList()))
                .thenReturn(List.of(student, extra));
        when(enrollmentRepository.findBySchoolIdAndAcademicYearId(TestSecurity.SCHOOL_ID, nextYearId))
                .thenReturn(List.of());
        when(enrollmentRepository.countBySectionIdAndAcademicYearIdAndStatus(nextSectionId, nextYearId, "ACTIVE"))
                .thenReturn(0L);
        when(sectionRepository.findBySchoolIdOrderByNameAsc(TestSecurity.SCHOOL_ID))
                .thenReturn(List.of(sourceSection, destSection));

        var preview = service.previewPromotion(new PromotionPreviewRequest(
                yearId, classId, sectionId, nextYearId, nextClassId, nextSectionId, null, null));

        assertEquals(1, preview.eligibleCount());
        assertEquals(1, preview.skippedCount());
        assertEquals(PromotionOutcome.PROMOTED, preview.students().get(0).outcome());
        assertEquals(PromotionOutcome.SKIPPED, preview.students().get(1).outcome());
        assertEquals("promotion.section_full", preview.students().get(1).reason());
    }

    @Test
    void executePromotionCreatesDestinationEnrollments() {
        AcademicYear source = year(yearId, "2025-26", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31), true);
        AcademicYear dest = year(nextYearId, "2026-27", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31), false);
        SchoolClass sourceClass = schoolClass(classId, "Class 5");
        SchoolClass destClass = schoolClass(nextClassId, "Class 6");
        Section sourceSection = section(sectionId, classId, "A", 40);
        Section destSection = section(nextSectionId, nextClassId, "A", 40);
        Student student = student(studentId, "S-001", "Ali", "Khan");
        StudentEnrollment enrollment = enrollment(studentId, sectionId, yearId, 7);

        when(academicYearRepository.findByIdAndSchoolId(yearId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(source));
        when(academicYearRepository.findByIdAndSchoolId(nextYearId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(dest));
        when(classRepository.findByIdAndSchoolId(classId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(sourceClass));
        when(classRepository.findByIdAndSchoolId(nextClassId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(destClass));
        when(sectionRepository.findByIdAndSchoolId(sectionId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(sourceSection));
        when(sectionRepository.findByIdAndSchoolId(nextSectionId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(destSection));
        when(enrollmentRepository.findBySchoolIdAndAcademicYearIdAndStatusAndSectionId(
                TestSecurity.SCHOOL_ID, yearId, "ACTIVE", sectionId))
                .thenReturn(List.of(enrollment));
        when(studentRepository.findBySchoolIdAndIdIn(eq(TestSecurity.SCHOOL_ID), anyList()))
                .thenReturn(List.of(student));
        when(enrollmentRepository.findBySchoolIdAndAcademicYearId(TestSecurity.SCHOOL_ID, nextYearId))
                .thenReturn(List.of());
        when(enrollmentRepository.countBySectionIdAndAcademicYearIdAndStatus(nextSectionId, nextYearId, "ACTIVE"))
                .thenReturn(0L);
        when(sectionRepository.findBySchoolIdOrderByNameAsc(TestSecurity.SCHOOL_ID))
                .thenReturn(List.of(sourceSection, destSection));
        when(promotionBatchRepository.save(any(PromotionBatch.class))).thenAnswer(inv -> {
            PromotionBatch batch = inv.getArgument(0);
            if (batch.getId() == null) {
                batch.setId(UUID.randomUUID());
            }
            return batch;
        });
        when(enrollmentRepository.save(any(StudentEnrollment.class))).thenAnswer(inv -> {
            StudentEnrollment e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(UUID.randomUUID());
            }
            return e;
        });
        when(promotionItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.executePromotion(new PromotionPreviewRequest(
                yearId, classId, sectionId, nextYearId, nextClassId, nextSectionId, List.of(studentId), null));

        assertEquals(1, result.promotedCount());
        assertEquals(0, result.skippedCount());
        assertEquals("PROMOTED", enrollment.getStatus());
        verify(enrollmentRepository, org.mockito.Mockito.times(2)).save(any(StudentEnrollment.class));
    }

    @Test
    void previewRejectsSameYear() {
        AcademicYear year = year(yearId, "2025-26", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31), true);
        when(academicYearRepository.findByIdAndSchoolId(yearId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(year));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.previewPromotion(
                new PromotionPreviewRequest(yearId, classId, sectionId, yearId, nextClassId, nextSectionId, null, null)));
        assertEquals("promotion.same_year", ex.getCode());
    }

    private static AcademicYear year(UUID id, String name, LocalDate start, LocalDate end, boolean current) {
        AcademicYear year = new AcademicYear();
        year.setId(id);
        year.setName(name);
        year.setStartDate(start);
        year.setEndDate(end);
        year.setCurrent(current);
        return year;
    }

    private static AcademicTerm term(UUID id, UUID yearId, String name, LocalDate start, LocalDate end) {
        AcademicTerm term = new AcademicTerm();
        term.setId(id);
        term.setAcademicYearId(yearId);
        term.setName(name);
        term.setStartDate(start);
        term.setEndDate(end);
        term.setStatus(AcademicTermStatus.PLANNED);
        return term;
    }

    private static SchoolClass schoolClass(UUID id, String name) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(id);
        schoolClass.setName(name);
        return schoolClass;
    }

    private static Section section(UUID id, UUID classId, String name, int capacity) {
        Section section = new Section();
        section.setId(id);
        section.setClassId(classId);
        section.setName(name);
        section.setCapacity(capacity);
        return section;
    }

    private static Student student(UUID id, String admissionNo, String first, String last) {
        Student student = new Student();
        student.setId(id);
        student.setAdmissionNo(admissionNo);
        student.setFirstName(first);
        student.setLastName(last);
        student.setStatus(StudentStatus.ACTIVE);
        return student;
    }

    private static StudentEnrollment enrollment(UUID studentId, UUID sectionId, UUID yearId, int roll) {
        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setId(UUID.randomUUID());
        enrollment.setStudentId(studentId);
        enrollment.setSectionId(sectionId);
        enrollment.setAcademicYearId(yearId);
        enrollment.setRollNumber(roll);
        enrollment.setStatus("ACTIVE");
        return enrollment;
    }
}
