package com.schoolms.academics;

import com.schoolms.TestSecurity;
import com.schoolms.academics.dto.AcademicYearRequest;
import com.schoolms.academics.dto.ClassRequest;
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

    private AcademicsService academicsService;

    @BeforeEach
    void setUp() {
        academicsService = new AcademicsService(academicYearRepository, classRepository,
                sectionRepository, subjectRepository, teacherRepository,
                teacherSubjectRepository, teacherSectionRepository, classSubjectRepository);
        TestSecurity.loginAsAdmin();
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void createClassPersistsWithSortOrder() {
        when(classRepository.save(any(SchoolClass.class))).thenAnswer(invocation -> {
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
        when(classSubjectRepository.findBySchoolId(TestSecurity.SCHOOL_ID)).thenReturn(java.util.List.of());
        when(teacherRepository.findBySchoolIdOrderByFirstNameAsc(TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.List.of());
        when(teacherSectionRepository.findBySchoolId(TestSecurity.SCHOOL_ID)).thenReturn(java.util.List.of());

        var dto = academicsService.createClass(new ClassRequest("Class 7", "C7", 7, null, null, null, null));

        assertEquals("Class 7", dto.name());
        assertEquals(7, dto.sortOrder());
        verify(classRepository).save(any(SchoolClass.class));
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
                () -> academicsService.createClass(new ClassRequest("Class 5", "C5", 5, "A", 40, null, null)));
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
}
