package com.schoolms.academics;

import com.schoolms.academics.dto.AcademicTermDto;
import com.schoolms.academics.dto.AcademicTermRequest;
import com.schoolms.academics.dto.AcademicYearDetailDto;
import com.schoolms.academics.dto.AcademicYearDto;
import com.schoolms.academics.dto.AcademicYearRequest;
import com.schoolms.academics.dto.PromotionBatchDto;
import com.schoolms.academics.dto.PromotionPreviewDto;
import com.schoolms.academics.dto.PromotionPreviewRequest;
import com.schoolms.academics.dto.PromotionStudentDto;
import com.schoolms.common.RecordIdGenerator;
import com.schoolms.common.enums.AcademicTermStatus;
import com.schoolms.common.enums.PromotionOutcome;
import com.schoolms.common.enums.StudentStatus;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.security.SecurityUtils;
import com.schoolms.student.Student;
import com.schoolms.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AcademicSessionService {

    private final AcademicYearRepository academicYearRepository;
    private final AcademicTermRepository termRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final PromotionBatchRepository promotionBatchRepository;
    private final PromotionBatchItemRepository promotionItemRepository;
    private final RecordIdGenerator recordIdGenerator;

    @Transactional(readOnly = true)
    public List<AcademicYearDetailDto> listSessions() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        List<AcademicYear> years = academicYearRepository.findBySchoolIdOrderByStartDateDesc(schoolId);
        return years.stream().map(year -> {
            List<AcademicTermDto> terms = termRepository
                    .findBySchoolIdAndAcademicYearIdOrderBySortOrderAscStartDateAsc(schoolId, year.getId())
                    .stream().map(AcademicTermDto::from).toList();
            return new AcademicYearDetailDto(AcademicYearDto.from(year), terms);
        }).toList();
    }

    @Transactional(readOnly = true)
    public AcademicYearDetailDto getYear(UUID yearId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        AcademicYear year = requireYear(yearId, schoolId);
        List<AcademicTermDto> terms = termRepository
                .findBySchoolIdAndAcademicYearIdOrderBySortOrderAscStartDateAsc(schoolId, yearId)
                .stream().map(AcademicTermDto::from).toList();
        return new AcademicYearDetailDto(AcademicYearDto.from(year), terms);
    }

    @Transactional
    public AcademicYearDto updateYear(UUID yearId, AcademicYearRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        AcademicYear year = requireYear(yearId, schoolId);
        String name = request.name().trim();
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException("academic_year.invalid_dates");
        }
        if (academicYearRepository.existsBySchoolIdAndNameAndIdNot(schoolId, name, yearId)) {
            throw new BusinessException("academic_year.name_exists");
        }
        List<AcademicTerm> terms = termRepository
                .findBySchoolIdAndAcademicYearIdOrderBySortOrderAscStartDateAsc(schoolId, yearId);
        for (AcademicTerm term : terms) {
            if (term.getStartDate().isBefore(request.startDate())
                    || term.getEndDate().isAfter(request.endDate())) {
                throw new BusinessException("academic_year.terms_outside_dates");
            }
        }
        year.setName(name);
        year.setStartDate(request.startDate());
        year.setEndDate(request.endDate());
        if (request.current() && !year.isCurrent()) {
            clearCurrentYear(schoolId);
            year.setCurrent(true);
        } else if (!request.current() && year.isCurrent()) {
            year.setCurrent(false);
        }
        return AcademicYearDto.from(academicYearRepository.save(year));
    }

    @Transactional
    public AcademicYearDto activateYear(UUID yearId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        AcademicYear year = requireYear(yearId, schoolId);
        if (!year.isCurrent()) {
            clearCurrentYear(schoolId);
            year.setCurrent(true);
            year = academicYearRepository.save(year);
        }
        return AcademicYearDto.from(year);
    }

    @Transactional
    public AcademicYearDto deactivateYear(UUID yearId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        AcademicYear year = requireYear(yearId, schoolId);
        year.setCurrent(false);
        return AcademicYearDto.from(academicYearRepository.save(year));
    }

    @Transactional
    public AcademicTermDto createTerm(UUID yearId, AcademicTermRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        AcademicYear year = requireYear(yearId, schoolId);
        String name = request.name().trim();
        AcademicTermStatus status = request.status() == null ? AcademicTermStatus.PLANNED : request.status();
        validateTermDates(year, request.startDate(), request.endDate());
        if (termRepository.existsBySchoolIdAndAcademicYearIdAndName(schoolId, yearId, name)) {
            throw new BusinessException("academic_term.name_exists");
        }
        List<AcademicTerm> existing = termRepository
                .findBySchoolIdAndAcademicYearIdOrderBySortOrderAscStartDateAsc(schoolId, yearId);
        assertNoOverlap(existing, null, request.startDate(), request.endDate());
        if (status == AcademicTermStatus.ACTIVE) {
            completeOtherActiveTerms(existing, null);
        }
        AcademicTerm term = new AcademicTerm();
        term.setSchoolId(schoolId);
        term.setAcademicYearId(yearId);
        term.setName(name);
        term.setStartDate(request.startDate());
        term.setEndDate(request.endDate());
        term.setStatus(status);
        term.setSortOrder(request.sortOrder() == null ? existing.size() + 1 : request.sortOrder());
        return AcademicTermDto.from(termRepository.save(term));
    }

    @Transactional
    public AcademicTermDto updateTerm(UUID termId, AcademicTermRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        AcademicTerm term = termRepository.findByIdAndSchoolId(termId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_term", termId));
        AcademicYear year = requireYear(term.getAcademicYearId(), schoolId);
        String name = request.name().trim();
        AcademicTermStatus status = request.status() == null ? term.getStatus() : request.status();
        validateTermDates(year, request.startDate(), request.endDate());
        if (termRepository.existsBySchoolIdAndAcademicYearIdAndNameAndIdNot(
                schoolId, term.getAcademicYearId(), name, termId)) {
            throw new BusinessException("academic_term.name_exists");
        }
        List<AcademicTerm> existing = termRepository
                .findBySchoolIdAndAcademicYearIdOrderBySortOrderAscStartDateAsc(schoolId, term.getAcademicYearId());
        assertNoOverlap(existing, termId, request.startDate(), request.endDate());
        if (status == AcademicTermStatus.ACTIVE) {
            completeOtherActiveTerms(existing, termId);
        }
        term.setName(name);
        term.setStartDate(request.startDate());
        term.setEndDate(request.endDate());
        term.setStatus(status);
        if (request.sortOrder() != null) {
            term.setSortOrder(request.sortOrder());
        }
        return AcademicTermDto.from(termRepository.save(term));
    }

    @Transactional
    public void deleteTerm(UUID termId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        AcademicTerm term = termRepository.findByIdAndSchoolId(termId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_term", termId));
        termRepository.delete(term);
    }

    @Transactional(readOnly = true)
    public PromotionPreviewDto previewPromotion(PromotionPreviewRequest request) {
        return buildPreview(request);
    }

    @Transactional
    public PromotionBatchDto executePromotion(PromotionPreviewRequest request) {
        PromotionPreviewDto preview = buildPreview(request);
        UUID schoolId = SecurityUtils.currentSchoolId();
        List<UUID> promotedIds = preview.students().stream()
                .filter(s -> s.outcome() == PromotionOutcome.PROMOTED)
                .map(PromotionStudentDto::studentId)
                .toList();
        if (promotedIds.isEmpty()) {
            throw new BusinessException("promotion.none_eligible");
        }

        Map<UUID, StudentEnrollment> sourceByStudent = loadSourceEnrollments(schoolId, request).stream()
                .collect(Collectors.toMap(StudentEnrollment::getStudentId, Function.identity()));

        PromotionBatch batch = new PromotionBatch();
        batch.setSchoolId(schoolId);
        batch.setSourceYearId(request.sourceYearId());
        batch.setDestinationYearId(request.destinationYearId());
        batch.setSourceClassId(request.sourceClassId());
        batch.setSourceSectionId(request.sourceSectionId());
        batch.setDestinationClassId(request.destinationClassId());
        batch.setDestinationSectionId(request.destinationSectionId());
        batch.setNotes(blankToNull(request.notes()));
        batch.setCreatedByUserId(SecurityUtils.currentUserId());
        batch.setPromotedCount(0);
        batch.setSkippedCount(0);
        batch = promotionBatchRepository.save(batch);

        int promoted = 0;
        int skipped = 0;
        int nextRoll = recordIdGenerator.nextRollNumber(
                request.destinationSectionId(), request.destinationYearId());
        List<PromotionBatchItem> items = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (PromotionStudentDto row : preview.students()) {
            PromotionBatchItem item = new PromotionBatchItem();
            item.setSchoolId(schoolId);
            item.setBatchId(batch.getId());
            item.setStudentId(row.studentId());
            item.setOutcome(row.outcome());
            item.setReason(row.reason());
            StudentEnrollment source = sourceByStudent.get(row.studentId());
            if (source != null) {
                item.setSourceEnrollmentId(source.getId());
            }
            if (row.outcome() == PromotionOutcome.PROMOTED && source != null) {
                source.setStatus("PROMOTED");
                enrollmentRepository.save(source);
                StudentEnrollment destination = new StudentEnrollment();
                destination.setSchoolId(schoolId);
                destination.setStudentId(row.studentId());
                destination.setSectionId(request.destinationSectionId());
                destination.setAcademicYearId(request.destinationYearId());
                destination.setRollNumber(nextRoll++);
                destination.setEnrollmentDate(today);
                destination.setStatus("ACTIVE");
                destination = enrollmentRepository.save(destination);
                item.setDestinationEnrollmentId(destination.getId());
                promoted++;
            } else {
                skipped++;
            }
            items.add(item);
        }
        promotionItemRepository.saveAll(items);
        batch.setPromotedCount(promoted);
        batch.setSkippedCount(skipped);
        batch = promotionBatchRepository.save(batch);
        return toBatchDto(batch, items, preview);
    }

    @Transactional(readOnly = true)
    public List<PromotionBatchDto> listPromotions() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        List<PromotionBatch> batches = promotionBatchRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId);
        if (batches.isEmpty()) {
            return List.of();
        }
        Map<UUID, AcademicYear> years = academicYearRepository.findBySchoolIdOrderByStartDateDesc(schoolId)
                .stream().collect(Collectors.toMap(AcademicYear::getId, Function.identity()));
        Map<UUID, SchoolClass> classes = classRepository.findBySchoolIdOrderBySortOrderAsc(schoolId)
                .stream().collect(Collectors.toMap(SchoolClass::getId, Function.identity()));
        Map<UUID, Section> sections = sectionRepository.findBySchoolIdOrderByNameAsc(schoolId)
                .stream().collect(Collectors.toMap(Section::getId, Function.identity()));
        return batches.stream()
                .map(batch -> PromotionBatchDto.from(
                        batch,
                        nameOfYear(years.get(batch.getSourceYearId())),
                        nameOfYear(years.get(batch.getDestinationYearId())),
                        nameOfClass(classes.get(batch.getSourceClassId())),
                        nameOfSection(sections.get(batch.getSourceSectionId())),
                        nameOfClass(classes.get(batch.getDestinationClassId())),
                        nameOfSection(sections.get(batch.getDestinationSectionId())),
                        List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PromotionBatchDto getPromotion(UUID batchId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        PromotionBatch batch = promotionBatchRepository.findByIdAndSchoolId(batchId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("promotion_batch", batchId));
        List<PromotionBatchItem> items = promotionItemRepository.findByBatchIdOrderByCreatedAtAsc(batchId);
        Map<UUID, Student> students = studentRepository.findBySchoolIdAndIdIn(schoolId,
                        items.stream().map(PromotionBatchItem::getStudentId).toList())
                .stream().collect(Collectors.toMap(Student::getId, Function.identity()));
        AcademicYear sourceYear = requireYear(batch.getSourceYearId(), schoolId);
        AcademicYear destYear = requireYear(batch.getDestinationYearId(), schoolId);
        SchoolClass sourceClass = requireClass(batch.getSourceClassId(), schoolId);
        SchoolClass destClass = requireClass(batch.getDestinationClassId(), schoolId);
        Section destSection = requireSection(batch.getDestinationSectionId(), schoolId);
        Section sourceSection = batch.getSourceSectionId() == null
                ? null
                : sectionRepository.findByIdAndSchoolId(batch.getSourceSectionId(), schoolId).orElse(null);
        List<PromotionStudentDto> rows = items.stream().map(item -> {
            Student student = students.get(item.getStudentId());
            return new PromotionStudentDto(
                    item.getStudentId(),
                    student == null ? "" : student.getAdmissionNo(),
                    student == null ? "" : student.getDisplayName(),
                    null,
                    sourceClass.getName(),
                    sourceSection == null ? null : sourceSection.getName(),
                    item.getOutcome(),
                    item.getReason());
        }).toList();
        return PromotionBatchDto.from(batch, sourceYear.getName(), destYear.getName(),
                sourceClass.getName(), sourceSection == null ? null : sourceSection.getName(),
                destClass.getName(), destSection.getName(), rows);
    }

    private PromotionPreviewDto buildPreview(PromotionPreviewRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        AcademicYear sourceYear = requireYear(request.sourceYearId(), schoolId);
        AcademicYear destYear = requireYear(request.destinationYearId(), schoolId);
        if (sourceYear.getId().equals(destYear.getId())) {
            throw new BusinessException("promotion.same_year");
        }
        SchoolClass sourceClass = requireClass(request.sourceClassId(), schoolId);
        SchoolClass destClass = requireClass(request.destinationClassId(), schoolId);
        Section destSection = requireSection(request.destinationSectionId(), schoolId);
        if (!destSection.getClassId().equals(destClass.getId())) {
            throw new BusinessException("promotion.section_class_mismatch");
        }
        Section sourceSection = null;
        if (request.sourceSectionId() != null) {
            sourceSection = requireSection(request.sourceSectionId(), schoolId);
            if (!sourceSection.getClassId().equals(sourceClass.getId())) {
                throw new BusinessException("promotion.section_class_mismatch");
            }
        }

        List<StudentEnrollment> sourceEnrollments = loadSourceEnrollments(schoolId, request);
        Set<UUID> selected = request.studentIds() == null || request.studentIds().isEmpty()
                ? null
                : new HashSet<>(request.studentIds());
        if (selected != null) {
            sourceEnrollments = sourceEnrollments.stream()
                    .filter(e -> selected.contains(e.getStudentId()))
                    .toList();
        }

        List<UUID> studentIds = sourceEnrollments.stream().map(StudentEnrollment::getStudentId).toList();
        Map<UUID, Student> students = studentIds.isEmpty()
                ? Map.of()
                : studentRepository.findBySchoolIdAndIdIn(schoolId, studentIds).stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));
        Set<UUID> destEnrolled = enrollmentRepository
                .findBySchoolIdAndAcademicYearId(schoolId, destYear.getId())
                .stream().map(StudentEnrollment::getStudentId).collect(Collectors.toSet());

        int occupied = (int) enrollmentRepository.countBySectionIdAndAcademicYearIdAndStatus(
                destSection.getId(), destYear.getId(), "ACTIVE");
        int capacity = destSection.getCapacity() == null ? Integer.MAX_VALUE : destSection.getCapacity();
        int remaining = Math.max(0, capacity - occupied);

        Map<UUID, Section> sourceSections = sectionRepository.findBySchoolIdOrderByNameAsc(schoolId)
                .stream().collect(Collectors.toMap(Section::getId, Function.identity()));

        List<PromotionStudentDto> rows = new ArrayList<>();
        int eligible = 0;
        int skipped = 0;
        for (StudentEnrollment enrollment : sourceEnrollments) {
            Student student = students.get(enrollment.getStudentId());
            Section fromSection = sourceSections.get(enrollment.getSectionId());
            String fromSectionName = fromSection == null ? null : fromSection.getName();
            String admissionNo = student == null ? "" : student.getAdmissionNo();
            String displayName = student == null ? "" : student.getDisplayName();
            Integer roll = enrollment.getRollNumber();
            PromotionOutcome outcome;
            String reason;
            if (student == null || student.getStatus() != StudentStatus.ACTIVE) {
                outcome = PromotionOutcome.SKIPPED;
                reason = "promotion.student_inactive";
            } else if (destEnrolled.contains(enrollment.getStudentId())) {
                outcome = PromotionOutcome.SKIPPED;
                reason = "promotion.already_enrolled";
            } else if (remaining <= 0) {
                outcome = PromotionOutcome.SKIPPED;
                reason = "promotion.section_full";
            } else {
                outcome = PromotionOutcome.PROMOTED;
                reason = null;
                remaining--;
                destEnrolled.add(enrollment.getStudentId());
            }
            if (outcome == PromotionOutcome.PROMOTED) {
                eligible++;
            } else {
                skipped++;
            }
            rows.add(new PromotionStudentDto(
                    enrollment.getStudentId(), admissionNo, displayName, roll,
                    sourceClass.getName(), fromSectionName, outcome, reason));
        }

        return new PromotionPreviewDto(
                sourceYear.getId(), sourceYear.getName(),
                destYear.getId(), destYear.getName(),
                sourceClass.getName(),
                sourceSection == null ? null : sourceSection.getName(),
                destClass.getName(), destSection.getName(),
                eligible, skipped, occupied, capacity == Integer.MAX_VALUE ? occupied + eligible : capacity,
                rows);
    }

    private List<StudentEnrollment> loadSourceEnrollments(UUID schoolId, PromotionPreviewRequest request) {
        if (request.sourceSectionId() != null) {
            return enrollmentRepository.findBySchoolIdAndAcademicYearIdAndStatusAndSectionId(
                    schoolId, request.sourceYearId(), "ACTIVE", request.sourceSectionId());
        }
        List<UUID> sectionIds = sectionRepository
                .findBySchoolIdAndClassIdOrderByNameAsc(schoolId, request.sourceClassId())
                .stream().map(Section::getId).toList();
        if (sectionIds.isEmpty()) {
            return List.of();
        }
        return enrollmentRepository.findBySchoolIdAndAcademicYearIdAndStatusAndSectionIdIn(
                schoolId, request.sourceYearId(), "ACTIVE", sectionIds);
    }

    private PromotionBatchDto toBatchDto(PromotionBatch batch, List<PromotionBatchItem> items,
                                         PromotionPreviewDto preview) {
        Map<UUID, PromotionStudentDto> byStudent = preview.students().stream()
                .collect(Collectors.toMap(PromotionStudentDto::studentId, Function.identity(),
                        (a, b) -> a, LinkedHashMap::new));
        List<PromotionStudentDto> rows = items.stream()
                .map(item -> {
                    PromotionStudentDto previewRow = byStudent.get(item.getStudentId());
                    if (previewRow != null) {
                        return previewRow;
                    }
                    return new PromotionStudentDto(item.getStudentId(), "", "", null,
                            preview.sourceClassName(), preview.sourceSectionName(),
                            item.getOutcome(), item.getReason());
                }).toList();
        return PromotionBatchDto.from(batch, preview.sourceYearName(), preview.destinationYearName(),
                preview.sourceClassName(), preview.sourceSectionName(),
                preview.destinationClassName(), preview.destinationSectionName(), rows);
    }

    private void validateTermDates(AcademicYear year, LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new BusinessException("academic_term.invalid_dates");
        }
        if (start.isBefore(year.getStartDate()) || end.isAfter(year.getEndDate())) {
            throw new BusinessException("academic_term.outside_year");
        }
    }

    private void assertNoOverlap(List<AcademicTerm> existing, UUID ignoreId, LocalDate start, LocalDate end) {
        for (AcademicTerm term : existing) {
            if (ignoreId != null && ignoreId.equals(term.getId())) {
                continue;
            }
            if (!end.isBefore(term.getStartDate()) && !term.getEndDate().isBefore(start)) {
                throw new BusinessException("academic_term.overlap");
            }
        }
    }

    private void completeOtherActiveTerms(List<AcademicTerm> existing, UUID keepId) {
        for (AcademicTerm term : existing) {
            if (keepId != null && keepId.equals(term.getId())) {
                continue;
            }
            if (term.getStatus() == AcademicTermStatus.ACTIVE) {
                term.setStatus(AcademicTermStatus.COMPLETED);
                termRepository.save(term);
            }
        }
    }

    private void clearCurrentYear(UUID schoolId) {
        academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId)
                .ifPresent(year -> {
                    year.setCurrent(false);
                    academicYearRepository.save(year);
                });
    }

    private AcademicYear requireYear(UUID yearId, UUID schoolId) {
        return academicYearRepository.findByIdAndSchoolId(yearId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_year", yearId));
    }

    private SchoolClass requireClass(UUID classId, UUID schoolId) {
        return classRepository.findByIdAndSchoolId(classId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("class", classId));
    }

    private Section requireSection(UUID sectionId, UUID schoolId) {
        return sectionRepository.findByIdAndSchoolId(sectionId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("section", sectionId));
    }

    private static String nameOfYear(AcademicYear year) {
        return year == null ? "" : year.getName();
    }

    private static String nameOfClass(SchoolClass schoolClass) {
        return schoolClass == null ? "" : schoolClass.getName();
    }

    private static String nameOfSection(Section section) {
        return section == null ? null : section.getName();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
