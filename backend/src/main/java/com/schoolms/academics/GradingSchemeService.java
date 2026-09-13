package com.schoolms.academics;

import com.schoolms.academics.dto.AssessmentWeightageDto;
import com.schoolms.academics.dto.AssessmentWeightageRequest;
import com.schoolms.academics.dto.GradeBoundaryDto;
import com.schoolms.academics.dto.GradeBoundaryRequest;
import com.schoolms.academics.dto.GradingSchemeDto;
import com.schoolms.academics.dto.GradingSchemeRequest;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GradingSchemeService {

    private static final Set<String> LEVELS = Set.of("PRIMARY", "MIDDLE", "SECONDARY", "SENIOR");
    private static final Set<String> EXAM_TYPES = Set.of("QUIZ", "UNIT", "MIDTERM", "TERM", "FINAL", "CONTINUOUS");
    private static final Set<String> SCALE_TYPES = Set.of("PERCENTAGE", "LETTER", "GPA");
    private static final Set<String> ASSESSMENT_TYPES = Set.of("QUIZ", "ASSIGNMENT", "MIDTERM", "PRACTICAL", "FINAL");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.01");

    private final GradingSchemeRepository schemeRepository;
    private final GradeBoundaryRepository boundaryRepository;
    private final AssessmentWeightageRepository weightageRepository;
    private final SchoolClassRepository classRepository;
    private final AcademicYearRepository academicYearRepository;

    @Transactional(readOnly = true)
    public List<GradingSchemeDto> list(UUID academicYearId, UUID classId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        if (academicYearId != null) {
            academicYearRepository.findByIdAndSchoolId(academicYearId, schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("academic_year", academicYearId));
        }
        if (classId != null) {
            classRepository.findByIdAndSchoolId(classId, schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("class", classId));
        }
        Map<UUID, SchoolClass> classes = indexClasses(schoolId);
        Map<UUID, AcademicYear> years = indexYears(schoolId);
        Map<UUID, List<GradeBoundary>> boundaries = indexBoundaries(schoolId);
        Map<UUID, List<AssessmentWeightage>> weightages = indexWeightages(schoolId);
        List<GradingScheme> schemes = academicYearId == null
                ? schemeRepository.findBySchoolIdOrderByNameAsc(schoolId)
                : schemeRepository.findBySchoolIdAndAcademicYearIdOrderByNameAsc(schoolId, academicYearId);
        return schemes.stream()
                .filter(scheme -> classId == null || classId.equals(scheme.getClassId()))
                .map(scheme -> toDto(scheme, classes, years, boundaries, weightages))
                .toList();
    }

    @Transactional
    public GradingSchemeDto create(GradingSchemeRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        validate(request, schoolId);
        String name = request.name().trim();
        if (schemeRepository.existsBySchoolIdAndAcademicYearIdAndClassIdAndName(
                schoolId, request.academicYearId(), request.classId(), name)) {
            throw new BusinessException("grading.name_exists");
        }
        GradingScheme scheme = new GradingScheme();
        apply(scheme, request, schoolId);
        maybeDeactivateCurrent(schoolId, scheme);
        scheme = schemeRepository.saveAndFlush(scheme);
        replaceChildren(scheme, request, schoolId);
        return loadDto(scheme, schoolId);
    }

    @Transactional
    public GradingSchemeDto update(UUID id, GradingSchemeRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        GradingScheme scheme = schemeRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("grading_scheme", id));
        validate(request, schoolId);
        String name = request.name().trim();
        if (schemeRepository.existsBySchoolIdAndAcademicYearIdAndClassIdAndNameAndIdNot(
                schoolId, request.academicYearId(), request.classId(), name, id)) {
            throw new BusinessException("grading.name_exists");
        }
        apply(scheme, request, schoolId);
        maybeDeactivateCurrent(schoolId, scheme);
        scheme = schemeRepository.saveAndFlush(scheme);
        replaceChildren(scheme, request, schoolId);
        return loadDto(scheme, schoolId);
    }

    @Transactional
    public GradingSchemeDto activate(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        GradingScheme scheme = schemeRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("grading_scheme", id));
        scheme.setStatus("ACTIVE");
        maybeDeactivateCurrent(schoolId, scheme);
        scheme = schemeRepository.saveAndFlush(scheme);
        return loadDto(scheme, schoolId);
    }

    @Transactional
    public GradingSchemeDto deactivate(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        GradingScheme scheme = schemeRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("grading_scheme", id));
        scheme.setStatus("INACTIVE");
        scheme = schemeRepository.saveAndFlush(scheme);
        return loadDto(scheme, schoolId);
    }

    private void validate(GradingSchemeRequest request, UUID schoolId) {
        academicYearRepository.findByIdAndSchoolId(request.academicYearId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_year", request.academicYearId()));
        classRepository.findByIdAndSchoolId(request.classId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("class", request.classId()));
        if (!LEVELS.contains(normalize(request.academicLevel()))) {
            throw new BusinessException("validation.invalid");
        }
        if (!EXAM_TYPES.contains(normalize(request.examType()))) {
            throw new BusinessException("validation.invalid");
        }
        if (!SCALE_TYPES.contains(normalize(request.scaleType()))) {
            throw new BusinessException("validation.invalid");
        }
        if (request.maxMarks().compareTo(BigDecimal.ZERO) <= 0
                || request.passMarks().compareTo(BigDecimal.ZERO) < 0
                || request.passMarks().compareTo(request.maxMarks()) > 0) {
            throw new BusinessException("grading.invalid_marks");
        }
        if (request.passPercent().compareTo(BigDecimal.ZERO) < 0
                || request.passPercent().compareTo(HUNDRED) > 0) {
            throw new BusinessException("grading.invalid_percent");
        }
        validateBoundaries(request.boundaries());
        validateWeightages(request.weightages());
    }

    private void validateBoundaries(List<GradeBoundaryRequest> boundaries) {
        if (boundaries == null || boundaries.isEmpty()) {
            throw new BusinessException("grading.boundaries_required");
        }
        Set<String> labels = new HashSet<>();
        for (GradeBoundaryRequest boundary : boundaries) {
            String label = boundary.label() == null ? "" : boundary.label().trim();
            if (label.isEmpty()) {
                throw new BusinessException("validation.not_blank");
            }
            if (!labels.add(label.toUpperCase())) {
                throw new BusinessException("grading.boundary_duplicate");
            }
            if (boundary.minPercent().compareTo(BigDecimal.ZERO) < 0
                    || boundary.maxPercent().compareTo(HUNDRED) > 0
                    || boundary.minPercent().compareTo(boundary.maxPercent()) > 0) {
                throw new BusinessException("grading.invalid_boundary");
            }
        }
    }

    private void validateWeightages(List<AssessmentWeightageRequest> weightages) {
        if (weightages == null || weightages.isEmpty()) {
            throw new BusinessException("grading.weightage_required");
        }
        Set<String> types = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (AssessmentWeightageRequest weightage : weightages) {
            String type = normalize(weightage.assessmentType());
            if (!ASSESSMENT_TYPES.contains(type)) {
                throw new BusinessException("grading.invalid_assessment");
            }
            if (!types.add(type)) {
                throw new BusinessException("grading.weightage_duplicate");
            }
            if (weightage.weightPercent().compareTo(BigDecimal.ZERO) <= 0
                    || weightage.weightPercent().compareTo(HUNDRED) > 0) {
                throw new BusinessException("grading.invalid_weightage");
            }
            total = total.add(weightage.weightPercent());
        }
        if (total.subtract(HUNDRED).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new BusinessException("grading.weightage_total");
        }
    }

    private void apply(GradingScheme scheme, GradingSchemeRequest request, UUID schoolId) {
        scheme.setSchoolId(schoolId);
        scheme.setAcademicYearId(request.academicYearId());
        scheme.setClassId(request.classId());
        scheme.setName(request.name().trim());
        scheme.setAcademicLevel(normalize(request.academicLevel()));
        scheme.setExamType(normalize(request.examType()));
        scheme.setScaleType(normalize(request.scaleType()));
        scheme.setPassMarks(request.passMarks());
        scheme.setPassPercent(request.passPercent());
        scheme.setMaxMarks(request.maxMarks());
        scheme.setEvaluationCriteria(blankToNull(request.evaluationCriteria()));
        scheme.setStatus(parseStatus(request.status()));
    }

    private void maybeDeactivateCurrent(UUID schoolId, GradingScheme scheme) {
        if (!"ACTIVE".equals(scheme.getStatus())) {
            return;
        }
        schemeRepository.findBySchoolIdAndAcademicYearIdAndClassIdAndStatus(
                        schoolId, scheme.getAcademicYearId(), scheme.getClassId(), "ACTIVE")
                .ifPresent(current -> {
                    if (!current.getId().equals(scheme.getId())) {
                        current.setStatus("INACTIVE");
                        schemeRepository.saveAndFlush(current);
                    }
                });
    }

    private void replaceChildren(GradingScheme scheme, GradingSchemeRequest request, UUID schoolId) {
        boundaryRepository.deleteBySchemeIdAndSchoolId(scheme.getId(), schoolId);
        weightageRepository.deleteBySchemeIdAndSchoolId(scheme.getId(), schoolId);
        boundaryRepository.flush();
        weightageRepository.flush();
        int order = 0;
        for (GradeBoundaryRequest item : request.boundaries()) {
            GradeBoundary boundary = new GradeBoundary();
            boundary.setSchoolId(schoolId);
            boundary.setSchemeId(scheme.getId());
            boundary.setLabel(item.label().trim());
            boundary.setMinPercent(item.minPercent());
            boundary.setMaxPercent(item.maxPercent());
            boundary.setGpaValue(item.gpaValue());
            boundary.setSortOrder(item.sortOrder() == null ? order : item.sortOrder());
            boundaryRepository.save(boundary);
            order++;
        }
        for (AssessmentWeightageRequest item : request.weightages()) {
            AssessmentWeightage weightage = new AssessmentWeightage();
            weightage.setSchoolId(schoolId);
            weightage.setSchemeId(scheme.getId());
            weightage.setAssessmentType(normalize(item.assessmentType()));
            weightage.setWeightPercent(item.weightPercent());
            weightageRepository.save(weightage);
        }
    }

    private GradingSchemeDto loadDto(GradingScheme scheme, UUID schoolId) {
        return toDto(scheme, indexClasses(schoolId), indexYears(schoolId),
                Map.of(scheme.getId(), boundaryRepository.findBySchemeIdAndSchoolIdOrderBySortOrderAsc(scheme.getId(), schoolId)),
                Map.of(scheme.getId(), weightageRepository.findBySchemeIdAndSchoolIdOrderByAssessmentTypeAsc(scheme.getId(), schoolId)));
    }

    private GradingSchemeDto toDto(GradingScheme scheme,
                                   Map<UUID, SchoolClass> classes,
                                   Map<UUID, AcademicYear> years,
                                   Map<UUID, List<GradeBoundary>> boundaries,
                                   Map<UUID, List<AssessmentWeightage>> weightages) {
        SchoolClass schoolClass = classes.get(scheme.getClassId());
        AcademicYear year = years.get(scheme.getAcademicYearId());
        List<GradeBoundaryDto> boundaryDtos = boundaries.getOrDefault(scheme.getId(), List.of()).stream()
                .map(item -> new GradeBoundaryDto(
                        item.getId(), item.getLabel(), item.getMinPercent(),
                        item.getMaxPercent(), item.getGpaValue(), item.getSortOrder()))
                .toList();
        List<AssessmentWeightageDto> weightageDtos = weightages.getOrDefault(scheme.getId(), List.of()).stream()
                .map(item -> new AssessmentWeightageDto(item.getId(), item.getAssessmentType(), item.getWeightPercent()))
                .toList();
        return new GradingSchemeDto(
                scheme.getId(),
                scheme.getAcademicYearId(),
                year == null ? null : year.getName(),
                scheme.getClassId(),
                schoolClass == null ? null : schoolClass.getName(),
                scheme.getName(),
                scheme.getAcademicLevel(),
                scheme.getExamType(),
                scheme.getScaleType(),
                scheme.getPassMarks(),
                scheme.getPassPercent(),
                scheme.getMaxMarks(),
                scheme.getEvaluationCriteria(),
                scheme.getStatus(),
                boundaryDtos,
                weightageDtos);
    }

    private Map<UUID, SchoolClass> indexClasses(UUID schoolId) {
        Map<UUID, SchoolClass> classes = new HashMap<>();
        for (SchoolClass schoolClass : classRepository.findBySchoolIdOrderBySortOrderAsc(schoolId)) {
            classes.put(schoolClass.getId(), schoolClass);
        }
        return classes;
    }

    private Map<UUID, AcademicYear> indexYears(UUID schoolId) {
        Map<UUID, AcademicYear> years = new HashMap<>();
        for (AcademicYear year : academicYearRepository.findBySchoolIdOrderByStartDateDesc(schoolId)) {
            years.put(year.getId(), year);
        }
        return years;
    }

    private Map<UUID, List<GradeBoundary>> indexBoundaries(UUID schoolId) {
        Map<UUID, List<GradeBoundary>> result = new HashMap<>();
        List<GradeBoundary> all = new ArrayList<>(boundaryRepository.findBySchoolId(schoolId));
        all.sort((a, b) -> Integer.compare(
                a.getSortOrder() == null ? 0 : a.getSortOrder(),
                b.getSortOrder() == null ? 0 : b.getSortOrder()));
        for (GradeBoundary boundary : all) {
            result.computeIfAbsent(boundary.getSchemeId(), key -> new ArrayList<>()).add(boundary);
        }
        return result;
    }

    private Map<UUID, List<AssessmentWeightage>> indexWeightages(UUID schoolId) {
        Map<UUID, List<AssessmentWeightage>> result = new HashMap<>();
        for (AssessmentWeightage weightage : weightageRepository.findBySchoolId(schoolId)) {
            result.computeIfAbsent(weightage.getSchemeId(), key -> new ArrayList<>()).add(weightage);
        }
        return result;
    }

    private String parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return "INACTIVE";
        }
        String value = normalize(status);
        if (!"ACTIVE".equals(value) && !"INACTIVE".equals(value)) {
            throw new BusinessException("validation.invalid");
        }
        return value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
