package com.schoolms.syllabus;

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
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.security.SecurityUtils;
import com.schoolms.syllabus.dto.SyllabusChapterDto;
import com.schoolms.syllabus.dto.SyllabusChapterRequest;
import com.schoolms.syllabus.dto.SyllabusChapterStatusRequest;
import com.schoolms.syllabus.dto.SyllabusCourseDetailDto;
import com.schoolms.syllabus.dto.SyllabusCourseDto;
import com.schoolms.syllabus.dto.SyllabusCourseRequest;
import com.schoolms.syllabus.dto.SyllabusMaterialDto;
import com.schoolms.syllabus.dto.SyllabusMaterialFile;
import com.schoolms.syllabus.dto.SyllabusUnitDto;
import com.schoolms.syllabus.dto.SyllabusUnitRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class SyllabusService {

    static final long MAX_MATERIAL_BYTES = 10L * 1024 * 1024;

    private static final String NOT_STARTED = "NOT_STARTED";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String COMPLETED = "COMPLETED";

    private final SyllabusCourseRepository courseRepository;
    private final SyllabusUnitRepository unitRepository;
    private final SyllabusChapterRepository chapterRepository;
    private final SyllabusMaterialRepository materialRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherProfileRepository teacherRepository;

    // ---- Courses --------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SyllabusCourseDto> list(UUID academicYearId, UUID classId, UUID sectionId,
                                        UUID subjectId, UUID teacherId, String progressStatus,
                                        String query) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        List<SyllabusCourse> courses = courseRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId).stream()
                .filter(course -> academicYearId == null || academicYearId.equals(course.getAcademicYearId()))
                .filter(course -> classId == null || classId.equals(course.getClassId()))
                .filter(course -> sectionId == null || sectionId.equals(course.getSectionId()))
                .filter(course -> subjectId == null || subjectId.equals(course.getSubjectId()))
                .filter(course -> teacherId == null || teacherId.equals(course.getTeacherId()))
                .toList();
        if (courses.isEmpty()) {
            return List.of();
        }
        References refs = references(schoolId);
        List<UUID> courseIds = courses.stream().map(SyllabusCourse::getId).toList();
        Map<UUID, List<SyllabusUnit>> unitsByCourse = unitsByCourse(schoolId, courseIds);
        Map<UUID, List<SyllabusChapter>> chaptersByUnit = chaptersByUnit(schoolId, unitsByCourse);
        String needle = normalize(query);
        String statusFilter = normalize(progressStatus);
        return courses.stream()
                .map(course -> toCourseDto(course, refs,
                        unitsByCourse.getOrDefault(course.getId(), List.of()), chaptersByUnit))
                .filter(dto -> statusFilter == null || statusFilter.equals(normalize(dto.progressStatus())))
                .filter(dto -> needle == null || matches(dto, needle))
                .toList();
    }

    @Transactional(readOnly = true)
    public SyllabusCourseDetailDto get(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        SyllabusCourse course = courseRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("syllabus_course", id));
        References refs = references(schoolId);
        List<SyllabusUnit> units = unitRepository
                .findBySchoolIdAndCourseIdOrderBySortOrderAscCreatedAtAsc(schoolId, id);
        Map<UUID, List<SyllabusChapter>> chaptersByUnit = chaptersByUnitOfUnits(schoolId, units);
        SyllabusCourseDto courseDto = toCourseDto(course, refs, units, chaptersByUnit);
        List<SyllabusUnitDto> unitDtos = units.stream()
                .map(unit -> toUnitDto(unit, chaptersByUnit.getOrDefault(unit.getId(), List.of())))
                .toList();
        List<SyllabusMaterialDto> materials = materialRepository
                .findBySchoolIdAndCourseIdOrderByCreatedAtDesc(schoolId, id).stream()
                .map(this::toMaterialDto)
                .toList();
        return new SyllabusCourseDetailDto(courseDto, unitDtos, materials);
    }

    @Transactional
    public SyllabusCourseDto create(SyllabusCourseRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        validateCourseRefs(schoolId, request);
        assertUniqueCourse(schoolId, request, null);
        SyllabusCourse course = new SyllabusCourse();
        course.setSchoolId(schoolId);
        applyCourse(course, request);
        SyllabusCourse saved = courseRepository.save(course);
        return toCourseDto(saved, references(schoolId), List.of(), Map.of());
    }

    @Transactional
    public SyllabusCourseDto update(UUID id, SyllabusCourseRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        SyllabusCourse course = courseRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("syllabus_course", id));
        validateCourseRefs(schoolId, request);
        assertUniqueCourse(schoolId, request, id);
        applyCourse(course, request);
        SyllabusCourse saved = courseRepository.save(course);
        List<SyllabusUnit> units = unitRepository
                .findBySchoolIdAndCourseIdOrderBySortOrderAscCreatedAtAsc(schoolId, id);
        return toCourseDto(saved, references(schoolId), units, chaptersByUnitOfUnits(schoolId, units));
    }

    @Transactional
    public void delete(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        SyllabusCourse course = courseRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("syllabus_course", id));
        courseRepository.delete(course);
    }

    // ---- Units ----------------------------------------------------------

    @Transactional
    public SyllabusUnitDto createUnit(UUID courseId, SyllabusUnitRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        courseRepository.findByIdAndSchoolId(courseId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("syllabus_course", courseId));
        SyllabusUnit unit = new SyllabusUnit();
        unit.setSchoolId(schoolId);
        unit.setCourseId(courseId);
        applyUnit(unit, request);
        return toUnitDto(unitRepository.save(unit), List.of());
    }

    @Transactional
    public SyllabusUnitDto updateUnit(UUID unitId, SyllabusUnitRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        SyllabusUnit unit = unitRepository.findByIdAndSchoolId(unitId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("syllabus_unit", unitId));
        applyUnit(unit, request);
        SyllabusUnit saved = unitRepository.save(unit);
        return toUnitDto(saved, chapterRepository
                .findBySchoolIdAndUnitIdOrderBySortOrderAscCreatedAtAsc(schoolId, unitId));
    }

    @Transactional
    public void deleteUnit(UUID unitId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        SyllabusUnit unit = unitRepository.findByIdAndSchoolId(unitId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("syllabus_unit", unitId));
        unitRepository.delete(unit);
    }

    // ---- Chapters -------------------------------------------------------

    @Transactional
    public SyllabusChapterDto createChapter(UUID unitId, SyllabusChapterRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        unitRepository.findByIdAndSchoolId(unitId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("syllabus_unit", unitId));
        SyllabusChapter chapter = new SyllabusChapter();
        chapter.setSchoolId(schoolId);
        chapter.setUnitId(unitId);
        applyChapter(chapter, request);
        applyCompletion(chapter, chapter.getStatus());
        return toChapterDto(chapterRepository.save(chapter));
    }

    @Transactional
    public SyllabusChapterDto updateChapter(UUID chapterId, SyllabusChapterRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        SyllabusChapter chapter = chapterRepository.findByIdAndSchoolId(chapterId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("syllabus_chapter", chapterId));
        applyChapter(chapter, request);
        if (request.status() != null) {
            applyCompletion(chapter, request.status());
        }
        return toChapterDto(chapterRepository.save(chapter));
    }

    @Transactional
    public SyllabusChapterDto updateChapterStatus(UUID chapterId, SyllabusChapterStatusRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        SyllabusChapter chapter = chapterRepository.findByIdAndSchoolId(chapterId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("syllabus_chapter", chapterId));
        applyCompletion(chapter, request.status());
        return toChapterDto(chapterRepository.save(chapter));
    }

    @Transactional
    public void deleteChapter(UUID chapterId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        SyllabusChapter chapter = chapterRepository.findByIdAndSchoolId(chapterId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("syllabus_chapter", chapterId));
        chapterRepository.delete(chapter);
    }

    // ---- Materials ------------------------------------------------------

    @Transactional
    public SyllabusMaterialDto addMaterial(UUID courseId, SyllabusMaterialKind kind, String title,
                                           UUID unitId, String externalUrl, MultipartFile file) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        courseRepository.findByIdAndSchoolId(courseId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("syllabus_course", courseId));
        if (!StringUtils.hasText(title)) {
            throw new BusinessException("syllabus.material_title_required");
        }
        SyllabusMaterialKind resolvedKind = kind == null ? SyllabusMaterialKind.DOCUMENT : kind;
        if (unitId != null) {
            SyllabusUnit unit = unitRepository.findByIdAndSchoolId(unitId, schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("syllabus_unit", unitId));
            if (!courseId.equals(unit.getCourseId())) {
                throw new BusinessException("syllabus.unit_not_in_course");
            }
        }
        boolean hasFile = file != null && !file.isEmpty();
        boolean hasUrl = StringUtils.hasText(externalUrl);
        if (resolvedKind == SyllabusMaterialKind.LINK && !hasUrl) {
            throw new BusinessException("syllabus.material_url_required");
        }
        if (!hasFile && !hasUrl) {
            throw new BusinessException("syllabus.material_source_required");
        }
        if (hasFile && file.getSize() > MAX_MATERIAL_BYTES) {
            throw new BusinessException("syllabus.material_too_large");
        }
        SyllabusMaterial material = new SyllabusMaterial();
        material.setSchoolId(schoolId);
        material.setCourseId(courseId);
        material.setUnitId(unitId);
        material.setKind(resolvedKind);
        material.setTitle(title.trim());
        material.setUploadedByUserId(SecurityUtils.currentUserId());
        if (hasUrl) {
            material.setExternalUrl(externalUrl.trim());
        }
        if (hasFile) {
            try {
                material.setContent(file.getBytes());
            } catch (IOException e) {
                throw new BusinessException("syllabus.material_upload_failed");
            }
            material.setFileName(StringUtils.hasText(file.getOriginalFilename())
                    ? file.getOriginalFilename() : title.trim());
            material.setContentType(StringUtils.hasText(file.getContentType())
                    ? file.getContentType() : "application/octet-stream");
            material.setFileSize(file.getSize());
        }
        return toMaterialDto(materialRepository.save(material));
    }

    @Transactional(readOnly = true)
    public SyllabusMaterialFile download(UUID materialId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        SyllabusMaterial material = materialRepository.findByIdAndSchoolId(materialId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("syllabus_material", materialId));
        if (material.getContent() == null || material.getContent().length == 0) {
            throw new BusinessException("syllabus.material_not_downloadable");
        }
        return new SyllabusMaterialFile(
                material.getFileName() == null ? material.getTitle() : material.getFileName(),
                material.getContentType() == null ? "application/octet-stream" : material.getContentType(),
                material.getContent());
    }

    @Transactional
    public void deleteMaterial(UUID materialId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        SyllabusMaterial material = materialRepository.findByIdAndSchoolId(materialId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("syllabus_material", materialId));
        materialRepository.delete(material);
    }

    // ---- Helpers --------------------------------------------------------

    private void validateCourseRefs(UUID schoolId, SyllabusCourseRequest request) {
        academicYearRepository.findByIdAndSchoolId(request.academicYearId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_year", request.academicYearId()));
        classRepository.findByIdAndSchoolId(request.classId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("class", request.classId()));
        subjectRepository.findByIdAndSchoolId(request.subjectId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("subject", request.subjectId()));
        if (request.sectionId() != null) {
            Section section = sectionRepository.findByIdAndSchoolId(request.sectionId(), schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("section", request.sectionId()));
            if (!section.getClassId().equals(request.classId())) {
                throw new BusinessException("syllabus.section_class_mismatch");
            }
        }
        if (request.teacherId() != null) {
            teacherRepository.findByIdAndSchoolId(request.teacherId(), schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("teacher", request.teacherId()));
        }
    }

    private void assertUniqueCourse(UUID schoolId, SyllabusCourseRequest request, UUID existingId) {
        boolean duplicate = courseRepository
                .findBySchoolIdAndAcademicYearIdAndClassIdAndSubjectId(
                        schoolId, request.academicYearId(), request.classId(), request.subjectId())
                .stream()
                .anyMatch(course -> !Objects.equals(course.getId(), existingId)
                        && Objects.equals(course.getSectionId(), request.sectionId()));
        if (duplicate) {
            throw new BusinessException("syllabus.course_exists");
        }
    }

    private void applyCourse(SyllabusCourse course, SyllabusCourseRequest request) {
        course.setAcademicYearId(request.academicYearId());
        course.setClassId(request.classId());
        course.setSectionId(request.sectionId());
        course.setSubjectId(request.subjectId());
        course.setTeacherId(request.teacherId());
        course.setTitle(request.title().trim());
        course.setDescription(trimToNull(request.description()));
        course.setTargetCompletionDate(request.targetCompletionDate());
        if (StringUtils.hasText(request.status())) {
            String status = request.status().trim().toUpperCase(Locale.ROOT);
            if (!status.equals("ACTIVE") && !status.equals("ARCHIVED")) {
                throw new BusinessException("syllabus.invalid_status");
            }
            course.setStatus(status);
        }
    }

    private void applyUnit(SyllabusUnit unit, SyllabusUnitRequest request) {
        unit.setTitle(request.title().trim());
        unit.setDescription(trimToNull(request.description()));
        unit.setSortOrder(request.sortOrder() == null ? nextUnitOrder(unit) : request.sortOrder());
        unit.setTargetCompletionDate(request.targetCompletionDate());
    }

    private int nextUnitOrder(SyllabusUnit unit) {
        if (unit.getId() != null && unit.getSortOrder() != null) {
            return unit.getSortOrder();
        }
        UUID schoolId = SecurityUtils.currentSchoolId();
        List<SyllabusUnit> siblings = unitRepository
                .findBySchoolIdAndCourseIdOrderBySortOrderAscCreatedAtAsc(schoolId, unit.getCourseId());
        return siblings.stream()
                .map(SyllabusUnit::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private void applyChapter(SyllabusChapter chapter, SyllabusChapterRequest request) {
        chapter.setTitle(request.title().trim());
        chapter.setDescription(trimToNull(request.description()));
        if (request.sortOrder() != null) {
            chapter.setSortOrder(request.sortOrder());
        } else if (chapter.getSortOrder() == null) {
            chapter.setSortOrder(0);
        }
        if (request.status() != null) {
            chapter.setStatus(request.status());
        }
        chapter.setTargetCompletionDate(request.targetCompletionDate());
    }

    private void applyCompletion(SyllabusChapter chapter, SyllabusChapterStatus status) {
        chapter.setStatus(status);
        if (status == SyllabusChapterStatus.COMPLETED) {
            if (chapter.getCompletedAt() == null) {
                chapter.setCompletedAt(Instant.now());
            }
            chapter.setCompletedByUserId(SecurityUtils.currentUserId());
        } else {
            chapter.setCompletedAt(null);
            chapter.setCompletedByUserId(null);
        }
    }

    private References references(UUID schoolId) {
        Map<UUID, AcademicYear> years = index(
                academicYearRepository.findBySchoolIdOrderByStartDateDesc(schoolId), AcademicYear::getId);
        Map<UUID, SchoolClass> classes = index(
                classRepository.findBySchoolIdOrderBySortOrderAsc(schoolId), SchoolClass::getId);
        Map<UUID, Section> sections = index(
                sectionRepository.findBySchoolIdOrderByNameAsc(schoolId), Section::getId);
        Map<UUID, Subject> subjects = index(
                subjectRepository.findBySchoolIdOrderByNameAsc(schoolId), Subject::getId);
        Map<UUID, TeacherProfile> teachers = index(
                teacherRepository.findBySchoolIdOrderByFirstNameAsc(schoolId), TeacherProfile::getId);
        return new References(years, classes, sections, subjects, teachers);
    }

    private <T> Map<UUID, T> index(List<T> items, Function<T, UUID> id) {
        Map<UUID, T> map = new HashMap<>();
        for (T item : items) {
            map.put(id.apply(item), item);
        }
        return map;
    }

    private Map<UUID, List<SyllabusUnit>> unitsByCourse(UUID schoolId, List<UUID> courseIds) {
        Map<UUID, List<SyllabusUnit>> map = new LinkedHashMap<>();
        for (SyllabusUnit unit : unitRepository
                .findBySchoolIdAndCourseIdInOrderBySortOrderAscCreatedAtAsc(schoolId, courseIds)) {
            map.computeIfAbsent(unit.getCourseId(), key -> new ArrayList<>()).add(unit);
        }
        return map;
    }

    private Map<UUID, List<SyllabusChapter>> chaptersByUnit(UUID schoolId, Map<UUID, List<SyllabusUnit>> unitsByCourse) {
        List<UUID> unitIds = unitsByCourse.values().stream()
                .flatMap(List::stream)
                .map(SyllabusUnit::getId)
                .toList();
        return chaptersByUnitOfUnitIds(schoolId, unitIds);
    }

    private Map<UUID, List<SyllabusChapter>> chaptersByUnitOfUnits(UUID schoolId, List<SyllabusUnit> units) {
        return chaptersByUnitOfUnitIds(schoolId, units.stream().map(SyllabusUnit::getId).toList());
    }

    private Map<UUID, List<SyllabusChapter>> chaptersByUnitOfUnitIds(UUID schoolId, List<UUID> unitIds) {
        Map<UUID, List<SyllabusChapter>> map = new LinkedHashMap<>();
        if (unitIds.isEmpty()) {
            return map;
        }
        for (SyllabusChapter chapter : chapterRepository
                .findBySchoolIdAndUnitIdInOrderBySortOrderAscCreatedAtAsc(schoolId, unitIds)) {
            map.computeIfAbsent(chapter.getUnitId(), key -> new ArrayList<>()).add(chapter);
        }
        return map;
    }

    private SyllabusCourseDto toCourseDto(SyllabusCourse course, References refs, List<SyllabusUnit> units,
                                          Map<UUID, List<SyllabusChapter>> chaptersByUnit) {
        List<SyllabusChapter> chapters = units.stream()
                .flatMap(unit -> chaptersByUnit.getOrDefault(unit.getId(), List.of()).stream())
                .toList();
        int totalChapters = chapters.size();
        int completedChapters = (int) chapters.stream()
                .filter(chapter -> chapter.getStatus() == SyllabusChapterStatus.COMPLETED)
                .count();
        int inProgressChapters = (int) chapters.stream()
                .filter(chapter -> chapter.getStatus() == SyllabusChapterStatus.IN_PROGRESS)
                .count();
        int completedUnits = (int) units.stream()
                .filter(unit -> {
                    List<SyllabusChapter> unitChapters = chaptersByUnit.getOrDefault(unit.getId(), List.of());
                    return !unitChapters.isEmpty() && unitChapters.stream()
                            .allMatch(chapter -> chapter.getStatus() == SyllabusChapterStatus.COMPLETED);
                })
                .count();
        String progressStatus = progressStatus(totalChapters, completedChapters, inProgressChapters);
        AcademicYear year = refs.years().get(course.getAcademicYearId());
        SchoolClass schoolClass = refs.classes().get(course.getClassId());
        Section section = course.getSectionId() == null ? null : refs.sections().get(course.getSectionId());
        Subject subject = refs.subjects().get(course.getSubjectId());
        TeacherProfile teacher = course.getTeacherId() == null ? null : refs.teachers().get(course.getTeacherId());
        return new SyllabusCourseDto(
                course.getId(),
                course.getAcademicYearId(),
                year == null ? null : year.getName(),
                course.getClassId(),
                schoolClass == null ? null : schoolClass.getName(),
                course.getSectionId(),
                section == null ? null : section.getName(),
                course.getSubjectId(),
                subject == null ? null : subject.getName(),
                course.getTeacherId(),
                teacher == null ? null : teacher.getDisplayName(),
                course.getTitle(),
                course.getDescription(),
                course.getTargetCompletionDate(),
                course.getStatus(),
                progressStatus,
                units.size(),
                completedUnits,
                totalChapters,
                completedChapters,
                inProgressChapters,
                totalChapters - completedChapters,
                percent(completedChapters, totalChapters),
                isOverdue(course.getTargetCompletionDate(), COMPLETED.equals(progressStatus)));
    }

    private SyllabusUnitDto toUnitDto(SyllabusUnit unit, List<SyllabusChapter> chapters) {
        int total = chapters.size();
        int completed = (int) chapters.stream()
                .filter(chapter -> chapter.getStatus() == SyllabusChapterStatus.COMPLETED)
                .count();
        boolean unitCompleted = total > 0 && completed == total;
        return new SyllabusUnitDto(
                unit.getId(),
                unit.getTitle(),
                unit.getDescription(),
                unit.getSortOrder() == null ? 0 : unit.getSortOrder(),
                unit.getTargetCompletionDate(),
                total,
                completed,
                percent(completed, total),
                isOverdue(unit.getTargetCompletionDate(), unitCompleted),
                chapters.stream().map(this::toChapterDto).toList());
    }

    private SyllabusChapterDto toChapterDto(SyllabusChapter chapter) {
        return new SyllabusChapterDto(
                chapter.getId(),
                chapter.getUnitId(),
                chapter.getTitle(),
                chapter.getDescription(),
                chapter.getSortOrder() == null ? 0 : chapter.getSortOrder(),
                chapter.getStatus().name(),
                chapter.getTargetCompletionDate(),
                chapter.getCompletedAt(),
                isOverdue(chapter.getTargetCompletionDate(),
                        chapter.getStatus() == SyllabusChapterStatus.COMPLETED));
    }

    private SyllabusMaterialDto toMaterialDto(SyllabusMaterial material) {
        return new SyllabusMaterialDto(
                material.getId(),
                material.getUnitId(),
                material.getKind().name(),
                material.getTitle(),
                material.getFileName(),
                material.getContentType(),
                material.getFileSize(),
                material.getExternalUrl(),
                material.getContent() != null && material.getContent().length > 0,
                material.getCreatedAt());
    }

    private boolean matches(SyllabusCourseDto dto, String needle) {
        return contains(dto.title(), needle)
                || contains(dto.description(), needle)
                || contains(dto.className(), needle)
                || contains(dto.sectionName(), needle)
                || contains(dto.subjectName(), needle)
                || contains(dto.teacherName(), needle);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static String progressStatus(int total, int completed, int inProgress) {
        if (total == 0) {
            return NOT_STARTED;
        }
        if (completed == total) {
            return COMPLETED;
        }
        if (completed > 0 || inProgress > 0) {
            return IN_PROGRESS;
        }
        return NOT_STARTED;
    }

    private static int percent(int completed, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round(completed * 100f / total);
    }

    private static boolean isOverdue(LocalDate target, boolean done) {
        return target != null && !done && target.isBefore(LocalDate.now());
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private record References(
            Map<UUID, AcademicYear> years,
            Map<UUID, SchoolClass> classes,
            Map<UUID, Section> sections,
            Map<UUID, Subject> subjects,
            Map<UUID, TeacherProfile> teachers
    ) {
    }
}
