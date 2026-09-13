package com.schoolms.academics;

import com.schoolms.TestSecurity;
import com.schoolms.academics.dto.AssessmentWeightageRequest;
import com.schoolms.academics.dto.GradeBoundaryRequest;
import com.schoolms.academics.dto.GradingSchemeRequest;
import com.schoolms.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradingSchemeServiceTest {

    @Mock
    private GradingSchemeRepository schemeRepository;
    @Mock
    private GradeBoundaryRepository boundaryRepository;
    @Mock
    private AssessmentWeightageRepository weightageRepository;
    @Mock
    private SchoolClassRepository classRepository;
    @Mock
    private AcademicYearRepository academicYearRepository;

    private GradingSchemeService service;

    @BeforeEach
    void setUp() {
        service = new GradingSchemeService(schemeRepository, boundaryRepository, weightageRepository,
                classRepository, academicYearRepository);
        TestSecurity.loginAsAdmin();
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void createRejectsWeightageNot100() {
        UUID yearId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        stubRefs(yearId, classId);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(request(yearId, classId, "INACTIVE",
                        List.of(weight("QUIZ", "40"), weight("FINAL", "50")))));
        assertEquals("grading.weightage_total", ex.getCode());
        verify(schemeRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsDuplicateName() {
        UUID yearId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        stubRefs(yearId, classId);
        when(schemeRepository.existsBySchoolIdAndAcademicYearIdAndClassIdAndName(
                TestSecurity.SCHOOL_ID, yearId, classId, "Class scheme")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(request(yearId, classId, "INACTIVE", defaultWeights())));
        assertEquals("grading.name_exists", ex.getCode());
        verify(schemeRepository, never()).saveAndFlush(any());
    }

    @Test
    void createPersistsActiveScheme() {
        UUID yearId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        stubRefs(yearId, classId);
        when(schemeRepository.existsBySchoolIdAndAcademicYearIdAndClassIdAndName(
                TestSecurity.SCHOOL_ID, yearId, classId, "Class scheme")).thenReturn(false);
        when(schemeRepository.findBySchoolIdAndAcademicYearIdAndClassIdAndStatus(
                TestSecurity.SCHOOL_ID, yearId, classId, "ACTIVE")).thenReturn(Optional.empty());
        when(schemeRepository.saveAndFlush(any(GradingScheme.class))).thenAnswer(invocation -> {
            GradingScheme scheme = invocation.getArgument(0);
            scheme.setId(UUID.randomUUID());
            return scheme;
        });
        AcademicYear year = new AcademicYear();
        year.setId(yearId);
        year.setName("2025-26");
        when(academicYearRepository.findBySchoolIdOrderByStartDateDesc(TestSecurity.SCHOOL_ID))
                .thenReturn(List.of(year));
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(classId);
        schoolClass.setName("Class 5");
        when(classRepository.findBySchoolIdOrderBySortOrderAsc(TestSecurity.SCHOOL_ID))
                .thenReturn(List.of(schoolClass));
        when(boundaryRepository.findBySchemeIdAndSchoolIdOrderBySortOrderAsc(any(), any()))
                .thenReturn(List.of());
        when(weightageRepository.findBySchemeIdAndSchoolIdOrderByAssessmentTypeAsc(any(), any()))
                .thenReturn(List.of());

        var dto = service.create(request(yearId, classId, "ACTIVE", defaultWeights()));

        assertEquals("Class 5", dto.className());
        assertEquals("ACTIVE", dto.status());
        verify(schemeRepository).saveAndFlush(any(GradingScheme.class));
        verify(boundaryRepository).deleteBySchemeIdAndSchoolId(any(), any());
        verify(weightageRepository).deleteBySchemeIdAndSchoolId(any(), any());
    }

    private void stubRefs(UUID yearId, UUID classId) {
        when(academicYearRepository.findByIdAndSchoolId(yearId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(new AcademicYear()));
        when(classRepository.findByIdAndSchoolId(classId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(new SchoolClass()));
    }

    private GradingSchemeRequest request(UUID yearId, UUID classId, String status,
                                         List<AssessmentWeightageRequest> weights) {
        return new GradingSchemeRequest(
                yearId, classId, "Class scheme", "PRIMARY", "TERM", "LETTER",
                new BigDecimal("33"), new BigDecimal("33"), new BigDecimal("100"),
                "Term evaluation", status, defaultBoundaries(), weights);
    }

    private List<GradeBoundaryRequest> defaultBoundaries() {
        return List.of(
                new GradeBoundaryRequest("A", new BigDecimal("80"), new BigDecimal("100"), new BigDecimal("4"), 1),
                new GradeBoundaryRequest("F", new BigDecimal("0"), new BigDecimal("32.99"), BigDecimal.ZERO, 2));
    }

    private List<AssessmentWeightageRequest> defaultWeights() {
        return List.of(weight("QUIZ", "20"), weight("MIDTERM", "30"), weight("FINAL", "50"));
    }

    private AssessmentWeightageRequest weight(String type, String percent) {
        return new AssessmentWeightageRequest(type, new BigDecimal(percent));
    }
}
