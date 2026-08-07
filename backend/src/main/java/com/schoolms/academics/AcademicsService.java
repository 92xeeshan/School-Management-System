package com.schoolms.academics;

import com.schoolms.academics.dto.AcademicYearDto;
import com.schoolms.academics.dto.AcademicYearRequest;
import com.schoolms.academics.dto.ClassDto;
import com.schoolms.academics.dto.ClassRequest;
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

import java.util.List;
import java.util.Map;
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
        return classRepository.findBySchoolIdOrderBySortOrderAsc(SecurityUtils.currentSchoolId())
                .stream().map(ClassDto::from).toList();
    }

    @Transactional
    public ClassDto createClass(ClassRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        if (classRepository.existsBySchoolIdAndName(schoolId, request.name())) {
            throw new BusinessException("error.conflict");
        }
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setSchoolId(schoolId);
        schoolClass.setName(request.name());
        schoolClass.setCode(request.code());
        schoolClass.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        return ClassDto.from(classRepository.save(schoolClass));
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
        return sections.stream()
                .map(section -> SectionDto.from(section, classNames.get(section.getClassId())))
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
        return SectionDto.from(section, schoolClass.getName());
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
