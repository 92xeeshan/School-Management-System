package com.schoolms.academics;

import com.schoolms.academics.dto.AcademicYearDto;
import com.schoolms.academics.dto.AcademicYearRequest;
import com.schoolms.academics.dto.ClassDto;
import com.schoolms.academics.dto.ClassRequest;
import com.schoolms.academics.dto.ClassUpdateRequest;
import com.schoolms.academics.dto.SectionDto;
import com.schoolms.academics.dto.SectionRequest;
import com.schoolms.academics.dto.SubjectDto;
import com.schoolms.academics.dto.SubjectRequest;
import com.schoolms.academics.dto.TeacherDto;
import com.schoolms.academics.dto.TeacherRequest;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.common.enums.SubjectType;
import com.schoolms.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AcademicsService {

    private final AcademicYearRepository academicYearRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherProfileRepository teacherRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final TeacherSectionRepository teacherSectionRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final StudentEnrollmentRepository enrollmentRepository;

    // ---- Academic years -------------------------------------------------
    @Transactional(readOnly = true)
    public List<AcademicYearDto> listYears() {
        return academicYearRepository.findBySchoolIdOrderByStartDateDesc(SecurityUtils.currentSchoolId())
                .stream().map(AcademicYearDto::from).toList();
    }

    @Transactional
    public AcademicYearDto createYear(AcademicYearRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        if (academicYearRepository.existsBySchoolIdAndName(schoolId, request.name())) {
            throw new BusinessException("error.conflict");
        }
        AcademicYear year = new AcademicYear();
        year.setSchoolId(schoolId);
        year.setName(request.name());
        year.setStartDate(request.startDate());
        year.setEndDate(request.endDate());
        if (request.current()) {
            clearCurrentYear(schoolId);
        }
        year.setCurrent(request.current());
        return AcademicYearDto.from(academicYearRepository.save(year));
    }

    // ---- Classes ----------------------------------------------------------
    @Transactional(readOnly = true)
    public List<ClassDto> listClasses() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        List<SchoolClass> classes = classRepository.findBySchoolIdOrderBySortOrderAsc(schoolId);
        Map<UUID, List<Section>> sectionsByClass = sectionRepository.findBySchoolIdOrderByNameAsc(schoolId)
                .stream().collect(Collectors.groupingBy(Section::getClassId));
        Map<UUID, List<SubjectDto>> subjectsByClass = subjectsByClass(schoolId);
        Map<UUID, TeacherProfile> teachers = teacherRepository.findBySchoolIdOrderByFirstNameAsc(schoolId)
                .stream().collect(Collectors.toMap(TeacherProfile::getId, t -> t));
        Map<UUID, UUID> classTeacherBySection = classTeacherIdsBySection(schoolId);
        Map<UUID, Integer> enrollmentBySection = enrollmentCountsBySection(schoolId);
        return classes.stream().map(schoolClass -> {
            List<SectionDto> sections = sectionsByClass.getOrDefault(schoolClass.getId(), List.of()).stream()
                    .map(section -> toSectionDto(section, schoolClass.getName(), classTeacherBySection,
                            teachers, enrollmentBySection))
                    .toList();
            List<SubjectDto> subjects = subjectsByClass.getOrDefault(schoolClass.getId(), List.of());
            return new ClassDto(schoolClass.getId(), schoolClass.getName(), schoolClass.getCode(),
                    schoolClass.getSortOrder(), sections, subjects);
        }).toList();
    }

    @Transactional
    public ClassDto createClass(ClassRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        String className = request.name().trim();
        String sectionName = normalizeSectionName(request.sectionName());
        Optional<SchoolClass> existing = classRepository.findBySchoolIdAndName(schoolId, className);
        SchoolClass schoolClass;
        if (existing.isPresent()) {
            schoolClass = existing.get();
            if (sectionName == null) {
                throw new BusinessException("class.section_required");
            }
            if (sectionRepository.existsByClassIdAndName(schoolClass.getId(), sectionName)) {
                throw new BusinessException("section.exists");
            }
        } else {
            schoolClass = new SchoolClass();
            schoolClass.setSchoolId(schoolId);
            schoolClass.setName(className);
            schoolClass.setCode(request.code() == null || request.code().isBlank() ? className : request.code().trim());
            schoolClass.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
            schoolClass = classRepository.saveAndFlush(schoolClass);
        }

        if (sectionName != null) {
            if (sectionRepository.existsByClassIdAndName(schoolClass.getId(), sectionName)) {
                throw new BusinessException("section.exists");
            }
            Section section = new Section();
            section.setSchoolId(schoolId);
            section.setClassId(schoolClass.getId());
            section.setName(sectionName);
            section.setCapacity(request.capacity() == null ? 40 : request.capacity());
            section.setRoom(blankToNull(request.room()));
            section = sectionRepository.saveAndFlush(section);
            assignClassTeacher(schoolId, section.getId(), request.classTeacherId());
        }

        assignSubjects(schoolId, schoolClass.getId(), request.subjectIds());
        return toClassDto(schoolClass, schoolId);
    }

    @Transactional
    public ClassDto updateClass(UUID classId, ClassUpdateRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        SchoolClass schoolClass = classRepository.findByIdAndSchoolId(classId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("school_class", classId));
        String className = request.name().trim();
        if (classRepository.existsBySchoolIdAndNameAndIdNot(schoolId, className, classId)) {
            throw new BusinessException("class.name_exists");
        }
        schoolClass.setName(className);
        if (request.code() != null && !request.code().isBlank()) {
            schoolClass.setCode(request.code().trim());
        }
        classRepository.saveAndFlush(schoolClass);

        if (request.sectionId() != null) {
            Section section = sectionRepository.findByIdAndSchoolId(request.sectionId(), schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("section", request.sectionId()));
            if (!section.getClassId().equals(classId)) {
                throw new BusinessException("section.not_found");
            }
            String sectionName = normalizeSectionName(request.sectionName());
            if (sectionName == null) {
                throw new BusinessException("section.invalid");
            }
            if (sectionRepository.existsByClassIdAndNameAndIdNot(classId, sectionName, section.getId())) {
                throw new BusinessException("section.exists");
            }
            section.setName(sectionName);
            if (request.capacity() != null) {
                int enrolled = enrollmentCount(section.getId(), schoolId);
                if (request.capacity() < enrolled) {
                    throw new BusinessException("section.capacity_below_enrollment");
                }
                section.setCapacity(request.capacity());
            }
            section.setRoom(blankToNull(request.room()));
            sectionRepository.saveAndFlush(section);
            replaceClassTeacher(schoolId, section.getId(), request.classTeacherId());
        }

        if (request.subjectIds() != null) {
            replaceSubjects(schoolId, classId, request.subjectIds());
        }
        return toClassDto(schoolClass, schoolId);
    }

    @Transactional
    public void deleteClass(UUID classId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        SchoolClass schoolClass = classRepository.findByIdAndSchoolId(classId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("school_class", classId));
        List<Section> sections = sectionRepository.findBySchoolIdAndClassIdOrderByNameAsc(schoolId, classId);
        for (Section section : sections) {
            if (enrollmentCount(section.getId(), schoolId) > 0) {
                throw new BusinessException("class.has_students");
            }
        }
        classRepository.delete(schoolClass);
    }

    // ---- Sections -----------------------------------------------------------
    @Transactional(readOnly = true)
    public List<SectionDto> listSections(UUID classId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        Map<UUID, String> classNames = classRepository.findBySchoolIdOrderBySortOrderAsc(schoolId)
                .stream().collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getName));
        List<Section> sections = classId == null
                ? sectionRepository.findBySchoolIdOrderByNameAsc(schoolId)
                : sectionRepository.findBySchoolIdAndClassIdOrderByNameAsc(schoolId, classId);
        Map<UUID, TeacherProfile> teachers = teacherRepository.findBySchoolIdOrderByFirstNameAsc(schoolId)
                .stream().collect(Collectors.toMap(TeacherProfile::getId, t -> t));
        Map<UUID, UUID> classTeacherBySection = classTeacherIdsBySection(schoolId);
        Map<UUID, Integer> enrollmentBySection = enrollmentCountsBySection(schoolId);
        return sections.stream()
                .map(section -> toSectionDto(section, classNames.get(section.getClassId()),
                        classTeacherBySection, teachers, enrollmentBySection))
                .toList();
    }

    @Transactional
    public SectionDto createSection(SectionRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        SchoolClass schoolClass = classRepository.findByIdAndSchoolId(request.classId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("school_class", request.classId()));
        if (sectionRepository.existsByClassIdAndName(request.classId(), request.name())) {
            throw new BusinessException("error.conflict");
        }
        Section section = new Section();
        section.setSchoolId(schoolId);
        section.setClassId(schoolClass.getId());
        section.setName(request.name());
        section.setCapacity(request.capacity() == null ? 40 : request.capacity());
        section = sectionRepository.save(section);
        return toSectionDto(section, schoolClass.getName(), classTeacherIdsBySection(schoolId),
                teacherRepository.findBySchoolIdOrderByFirstNameAsc(schoolId)
                        .stream().collect(Collectors.toMap(TeacherProfile::getId, t -> t)),
                enrollmentCountsBySection(schoolId));
    }

    @Transactional
    public void deleteSection(UUID sectionId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        Section section = sectionRepository.findByIdAndSchoolId(sectionId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("section", sectionId));
        if (enrollmentCount(section.getId(), schoolId) > 0) {
            throw new BusinessException("section.has_students");
        }
        UUID classId = section.getClassId();
        sectionRepository.delete(section);
        if (sectionRepository.findBySchoolIdAndClassIdOrderByNameAsc(schoolId, classId).isEmpty()) {
            classRepository.findByIdAndSchoolId(classId, schoolId).ifPresent(classRepository::delete);
        }
    }

    // ---- Subjects ------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<SubjectDto> listSubjects() {
        return subjectRepository.findBySchoolIdOrderByNameAsc(SecurityUtils.currentSchoolId())
                .stream().map(SubjectDto::from).toList();
    }

    @Transactional
    public SubjectDto createSubject(SubjectRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        if (subjectRepository.existsBySchoolIdAndName(schoolId, request.name())) {
            throw new BusinessException("error.conflict");
        }
        Subject subject = new Subject();
        subject.setSchoolId(schoolId);
        subject.setName(request.name());
        subject.setCode(request.code());
        subject.setType(parseSubjectType(request.type()));
        subject.setDescription(request.description());
        return SubjectDto.from(subjectRepository.save(subject));
    }

    // ---- Teachers ------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<TeacherDto> listTeachers() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        return teacherRepository.findBySchoolIdOrderByFirstNameAsc(schoolId).stream()
                .map(teacher -> withAssignments(teacher, schoolId))
                .toList();
    }

    @Transactional(readOnly = true)
    public TeacherDto getTeacher(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        TeacherProfile teacher = teacherRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("teacher", id));
        return withAssignments(teacher, schoolId);
    }

    @Transactional
    public TeacherDto createTeacher(TeacherRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        if (teacherRepository.existsBySchoolIdAndEmployeeNo(schoolId, request.employeeNo())) {
            throw new BusinessException("error.conflict");
        }
        TeacherProfile teacher = new TeacherProfile();
        teacher.setSchoolId(schoolId);
        apply(teacher, request);
        teacher = teacherRepository.save(teacher);
        saveAssignments(teacher, schoolId, request);
        return withAssignments(teacher, schoolId);
    }

    private void apply(TeacherProfile teacher, TeacherRequest request) {
        teacher.setEmployeeNo(request.employeeNo());
        teacher.setFirstName(request.firstName());
        teacher.setLastName(request.lastName());
        teacher.setEmail(request.email());
        teacher.setPhone(request.phone());
        teacher.setDesignation(request.designation());
        teacher.setQualification(request.qualification());
        teacher.setJoinDate(request.joinDate());
        teacher.setUserId(request.userId());
        teacher.setStatus("ACTIVE");
    }

    private void saveAssignments(TeacherProfile teacher, UUID schoolId, TeacherRequest request) {
        if (request.subjectIds() != null) {
            for (UUID subjectId : request.subjectIds()) {
                TeacherSubject ts = new TeacherSubject();
                ts.setSchoolId(schoolId);
                ts.setTeacherId(teacher.getId());
                ts.setSubjectId(subjectId);
                teacherSubjectRepository.save(ts);
            }
        }
        if (request.sectionIds() != null) {
            for (UUID sectionId : request.sectionIds()) {
                TeacherSection tsec = new TeacherSection();
                tsec.setSchoolId(schoolId);
                tsec.setTeacherId(teacher.getId());
                tsec.setSectionId(sectionId);
                tsec.setAcademicYearId(currentYearId(schoolId));
                tsec.setClassTeacher(false);
                teacherSectionRepository.save(tsec);
            }
        }
    }

    private TeacherDto withAssignments(TeacherProfile teacher, UUID schoolId) {
        List<UUID> subjectIds = teacherSubjectRepository
                .findByTeacherIdAndSchoolId(teacher.getId(), schoolId).stream()
                .map(TeacherSubject::getSubjectId).toList();
        List<UUID> sectionIds = teacherSectionRepository
                .findByTeacherIdAndSchoolId(teacher.getId(), schoolId).stream()
                .map(TeacherSection::getSectionId).toList();
        TeacherDto dto = TeacherDto.from(teacher);
        return new TeacherDto(dto.id(), dto.userId(), dto.employeeNo(), dto.firstName(),
                dto.lastName(), dto.displayName(), dto.email(), dto.phone(), dto.designation(),
                dto.qualification(), dto.joinDate(), dto.status(), subjectIds, sectionIds);
    }

    private void clearCurrentYear(UUID schoolId) {
        academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId)
                .ifPresent(year -> {
                    year.setCurrent(false);
                    academicYearRepository.save(year);
                });
    }

    private UUID currentYearId(UUID schoolId) {
        return academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId)
                .map(AcademicYear::getId).orElse(null);
    }

    private ClassDto toClassDto(SchoolClass schoolClass, UUID schoolId) {
        Map<UUID, TeacherProfile> teachers = teacherRepository.findBySchoolIdOrderByFirstNameAsc(schoolId)
                .stream().collect(Collectors.toMap(TeacherProfile::getId, t -> t));
        Map<UUID, UUID> classTeacherBySection = classTeacherIdsBySection(schoolId);
        Map<UUID, Integer> enrollmentBySection = enrollmentCountsBySection(schoolId);
        List<SectionDto> sections = sectionRepository
                .findBySchoolIdAndClassIdOrderByNameAsc(schoolId, schoolClass.getId()).stream()
                .map(section -> toSectionDto(section, schoolClass.getName(), classTeacherBySection,
                        teachers, enrollmentBySection))
                .toList();
        List<SubjectDto> subjects = subjectsForClass(schoolId, schoolClass.getId());
        return new ClassDto(schoolClass.getId(), schoolClass.getName(), schoolClass.getCode(),
                schoolClass.getSortOrder(), sections, subjects);
    }

    private SectionDto toSectionDto(Section section, String className,
                                    Map<UUID, UUID> classTeacherBySection,
                                    Map<UUID, TeacherProfile> teachers,
                                    Map<UUID, Integer> enrollmentBySection) {
        UUID teacherId = classTeacherBySection.get(section.getId());
        TeacherProfile teacher = teacherId == null ? null : teachers.get(teacherId);
        int studentCount = enrollmentBySection.getOrDefault(section.getId(), 0);
        return new SectionDto(section.getId(), section.getClassId(), className,
                section.getName(), section.getCapacity(), section.getRoom(), studentCount,
                teacherId, teacher == null ? null : teacher.getDisplayName());
    }

    private Map<UUID, List<SubjectDto>> subjectsByClass(UUID schoolId) {
        Map<UUID, Subject> subjects = subjectRepository.findBySchoolIdOrderByNameAsc(schoolId)
                .stream().collect(Collectors.toMap(Subject::getId, s -> s));
        Map<UUID, List<SubjectDto>> result = new HashMap<>();
        for (ClassSubject link : classSubjectRepository.findBySchoolId(schoolId)) {
            Subject subject = subjects.get(link.getSubjectId());
            if (subject == null) {
                continue;
            }
            result.computeIfAbsent(link.getClassId(), key -> new ArrayList<>()).add(SubjectDto.from(subject));
        }
        return result;
    }

    private Map<UUID, UUID> classTeacherIdsBySection(UUID schoolId) {
        Map<UUID, UUID> result = new HashMap<>();
        for (TeacherSection assignment : teacherSectionRepository.findBySchoolId(schoolId)) {
            if (assignment.isClassTeacher()) {
                result.putIfAbsent(assignment.getSectionId(), assignment.getTeacherId());
            }
        }
        return result;
    }

    private void assignSubjects(UUID schoolId, UUID classId, List<UUID> subjectIds) {
        if (subjectIds == null) {
            return;
        }
        Set<UUID> unique = subjectIds.stream().filter(id -> id != null).collect(Collectors.toSet());
        for (UUID subjectId : unique) {
            subjectRepository.findByIdAndSchoolId(subjectId, schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("subject", subjectId));
            if (!classSubjectRepository.existsByClassIdAndSubjectId(classId, subjectId)) {
                ClassSubject link = new ClassSubject();
                link.setSchoolId(schoolId);
                link.setClassId(classId);
                link.setSubjectId(subjectId);
                classSubjectRepository.saveAndFlush(link);
            }
        }
    }

    private void assignClassTeacher(UUID schoolId, UUID sectionId, UUID teacherId) {
        if (teacherId == null) {
            return;
        }
        replaceClassTeacher(schoolId, sectionId, teacherId);
    }

    private void replaceClassTeacher(UUID schoolId, UUID sectionId, UUID teacherId) {
        UUID yearId = currentYearId(schoolId);
        if (yearId == null) {
            throw new BusinessException("academic_year.not_found");
        }
        List<TeacherSection> assignments = teacherSectionRepository.findBySchoolId(schoolId);
        for (TeacherSection assignment : assignments) {
            if (assignment.getSectionId().equals(sectionId)
                    && assignment.getAcademicYearId().equals(yearId)
                    && assignment.isClassTeacher()) {
                assignment.setClassTeacher(false);
                teacherSectionRepository.saveAndFlush(assignment);
            }
        }
        if (teacherId == null) {
            return;
        }
        teacherRepository.findByIdAndSchoolId(teacherId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("teacher", teacherId));
        for (TeacherSection assignment : assignments) {
            if (assignment.getTeacherId().equals(teacherId)
                    && assignment.getAcademicYearId().equals(yearId)
                    && assignment.isClassTeacher()
                    && !assignment.getSectionId().equals(sectionId)) {
                throw new BusinessException("section.teacher_conflict");
            }
        }
        TeacherSection assignment = teacherSectionRepository
                .findByTeacherIdAndSectionIdAndAcademicYearId(teacherId, sectionId, yearId)
                .orElseGet(() -> {
                    TeacherSection created = new TeacherSection();
                    created.setSchoolId(schoolId);
                    created.setTeacherId(teacherId);
                    created.setSectionId(sectionId);
                    created.setAcademicYearId(yearId);
                    return created;
                });
        assignment.setClassTeacher(true);
        teacherSectionRepository.saveAndFlush(assignment);
    }

    private void replaceSubjects(UUID schoolId, UUID classId, List<UUID> subjectIds) {
        Set<UUID> unique = subjectIds.stream().filter(id -> id != null).collect(Collectors.toSet());
        for (UUID subjectId : unique) {
            subjectRepository.findByIdAndSchoolId(subjectId, schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("subject", subjectId));
        }
        List<ClassSubject> existing = classSubjectRepository.findByClassIdAndSchoolId(classId, schoolId);
        for (ClassSubject link : existing) {
            if (!unique.contains(link.getSubjectId())) {
                classSubjectRepository.delete(link);
            }
        }
        Set<UUID> already = existing.stream().map(ClassSubject::getSubjectId).collect(Collectors.toSet());
        for (UUID subjectId : unique) {
            if (!already.contains(subjectId)) {
                ClassSubject link = new ClassSubject();
                link.setSchoolId(schoolId);
                link.setClassId(classId);
                link.setSubjectId(subjectId);
                classSubjectRepository.saveAndFlush(link);
            }
        }
    }

    private Map<UUID, Integer> enrollmentCountsBySection(UUID schoolId) {
        UUID yearId = currentYearId(schoolId);
        Map<UUID, Integer> result = new HashMap<>();
        if (yearId == null) {
            return result;
        }
        for (StudentEnrollment enrollment : enrollmentRepository
                .findBySchoolIdAndAcademicYearIdAndStatus(schoolId, yearId, "ACTIVE")) {
            result.merge(enrollment.getSectionId(), 1, Integer::sum);
        }
        return result;
    }

    private int enrollmentCount(UUID sectionId, UUID schoolId) {
        UUID yearId = currentYearId(schoolId);
        if (yearId == null) {
            return 0;
        }
        return (int) enrollmentRepository.countBySectionIdAndAcademicYearIdAndStatus(sectionId, yearId, "ACTIVE");
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private List<SubjectDto> subjectsForClass(UUID schoolId, UUID classId) {
        Map<UUID, Subject> subjects = subjectRepository.findBySchoolIdOrderByNameAsc(schoolId)
                .stream().collect(Collectors.toMap(Subject::getId, s -> s));
        List<SubjectDto> result = new ArrayList<>();
        for (ClassSubject link : classSubjectRepository.findByClassIdAndSchoolId(classId, schoolId)) {
            Subject subject = subjects.get(link.getSubjectId());
            if (subject != null) {
                result.add(SubjectDto.from(subject));
            }
        }
        return result;
    }

    private String normalizeSectionName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String name = value.trim().toUpperCase();
        if (!Set.of("A", "B", "C").contains(name)) {
            throw new BusinessException("section.invalid");
        }
        return name;
    }

    private SubjectType parseSubjectType(String value) {
        if (value == null || value.isBlank()) {
            return SubjectType.CORE;
        }
        try {
            return SubjectType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("validation.invalid");
        }
    }
}
