package com.schoolms.staff;

import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.SchoolClass;
import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.academics.Section;
import com.schoolms.academics.SectionRepository;
import com.schoolms.academics.TeacherProfile;
import com.schoolms.academics.TeacherProfileRepository;
import com.schoolms.academics.TeacherSection;
import com.schoolms.academics.TeacherSectionRepository;
import com.schoolms.academics.TeacherSubject;
import com.schoolms.academics.TeacherSubjectRepository;
import com.schoolms.auth.CredentialAccountService;
import com.schoolms.common.api.PageRequests;
import com.schoolms.common.api.PagedResponse;
import com.schoolms.common.RecordIdGenerator;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.security.SecurityUtils;
import com.schoolms.user.dto.ProvisionedAccount;
import com.schoolms.staff.dto.ClassTeacherRequest;
import com.schoolms.staff.dto.SectionRefDto;
import com.schoolms.staff.dto.StaffMemberDto;
import com.schoolms.staff.dto.StaffMemberRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffService {

    private static final String ACTIVE = "ACTIVE";
    private static final String INACTIVE = "INACTIVE";
    private static final Map<String, String> DIRECTORY_SORT = Map.of(
            "employeeNo", "employeeNo",
            "name", "firstName",
            "designation", "designation",
            "department", "department",
            "status", "status");

    private final TeacherProfileRepository teacherRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final TeacherSectionRepository teacherSectionRepository;
    private final NonTeachingStaffRepository nonTeachingStaffRepository;
    private final SectionRepository sectionRepository;
    private final SchoolClassRepository classRepository;
    private final AcademicYearRepository academicYearRepository;
    private final CredentialAccountService credentialAccountService;
    private final RecordIdGenerator recordIdGenerator;

    // ---- Teaching staff --------------------------------------------------

    @Transactional(readOnly = true)
    public PagedResponse<StaffMemberDto> listTeachingStaff(String query, int page, int size,
                                                           String sort, String direction) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UUID yearId = currentYearIdOrNull(schoolId);
        Map<UUID, List<SectionRefDto>> classTeacherByTeacher = classTeacherRefsByTeacher(schoolId, yearId);
        String normalized = PageRequests.normalizeQuery(query);
        PageRequest pageable = PageRequests.of(page, size, sort, direction, "firstName", DIRECTORY_SORT);
        Page<TeacherProfile> result = teacherRepository.searchDirectory(
                schoolId, normalized, PageRequests.likePattern(normalized), pageable);
        return PagedResponse.from(result.map(teacher -> toDto(teacher, schoolId,
                classTeacherByTeacher.getOrDefault(teacher.getId(), List.of()))));
    }

    @Transactional
    public StaffMemberDto createTeachingStaff(StaffMemberRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        String employeeNo = resolveEmployeeNo(schoolId, request.employeeNo(), true);
        if (teacherRepository.existsBySchoolIdAndEmployeeNo(schoolId, employeeNo)) {
            throw new BusinessException("staff.employee_no_exists");
        }
        TeacherProfile teacher = new TeacherProfile();
        teacher.setSchoolId(schoolId);
        teacher.setStatus(ACTIVE);
        apply(teacher, request, employeeNo);
        ProvisionedAccount account = provisionIfNeeded(schoolId, "TEACHER", request);
        if (account != null) {
            teacher.setUserId(account.userId());
        }
        teacher = teacherRepository.save(teacher);
        return withCredentials(toDto(teacher, schoolId, List.of()), account);
    }

    @Transactional
    public StaffMemberDto updateTeachingStaff(UUID id, StaffMemberRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        TeacherProfile teacher = findTeacher(id, schoolId);
        String employeeNo = blankToNull(request.employeeNo());
        if (employeeNo == null) {
            employeeNo = teacher.getEmployeeNo();
        } else if (!employeeNo.equals(teacher.getEmployeeNo())
                && teacherRepository.existsBySchoolIdAndEmployeeNoAndIdNot(schoolId, employeeNo, id)) {
            throw new BusinessException("staff.employee_no_exists");
        }
        apply(teacher, request, employeeNo);
        teacher = teacherRepository.save(teacher);
        UUID yearId = currentYearIdOrNull(schoolId);
        return toDto(teacher, schoolId, classTeacherRefs(teacher, schoolId, yearId));
    }

    @Transactional
    public void deactivateTeachingStaff(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        TeacherProfile teacher = findTeacher(id, schoolId);
        teacher.setStatus(INACTIVE);
        teacherRepository.save(teacher);
        clearClassTeacher(teacher.getId(), schoolId);
    }

    @Transactional
    public StaffMemberDto assignClassTeacher(UUID teacherId, ClassTeacherRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        TeacherProfile teacher = findTeacher(teacherId, schoolId);
        UUID yearId = academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId)
                .map(AcademicYear::getId)
                .orElseThrow(() -> new BusinessException("academic_year.not_found"));
        clearClassTeacher(teacherId, schoolId);

        UUID sectionId = request == null ? null : request.sectionId();
        if (sectionId != null) {
            Section section = sectionRepository.findByIdAndSchoolId(sectionId, schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("section", sectionId));
            for (TeacherSection link : teacherSectionRepository.findBySectionIdAndSchoolId(sectionId, schoolId)) {
                if (link.getAcademicYearId().equals(yearId) && link.isClassTeacher()) {
                    link.setClassTeacher(false);
                    teacherSectionRepository.save(link);
                }
            }
            TeacherSection assignment = teacherSectionRepository
                    .findByTeacherIdAndSectionIdAndAcademicYearId(teacherId, section.getId(), yearId)
                    .orElseGet(() -> {
                        TeacherSection created = new TeacherSection();
                        created.setSchoolId(schoolId);
                        created.setTeacherId(teacherId);
                        created.setSectionId(section.getId());
                        created.setAcademicYearId(yearId);
                        return created;
                    });
            assignment.setClassTeacher(true);
            teacherSectionRepository.save(assignment);
        }
        return toDto(teacher, schoolId, classTeacherRefs(teacher, schoolId, yearId));
    }

    // ---- Non-teaching staff ----------------------------------------------

    @Transactional(readOnly = true)
    public PagedResponse<StaffMemberDto> listNonTeachingStaff(String query, int page, int size,
                                                              String sort, String direction) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        String normalized = PageRequests.normalizeQuery(query);
        PageRequest pageable = PageRequests.of(page, size, sort, direction, "firstName", DIRECTORY_SORT);
        Page<NonTeachingStaff> result = nonTeachingStaffRepository.searchDirectory(
                schoolId, normalized, PageRequests.likePattern(normalized), pageable);
        return PagedResponse.from(result.map(StaffMemberDto::from));
    }

    @Transactional
    public StaffMemberDto createNonTeachingStaff(StaffMemberRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        String employeeNo = resolveEmployeeNo(schoolId, request.employeeNo(), false);
        if (nonTeachingStaffRepository.existsBySchoolIdAndEmployeeNo(schoolId, employeeNo)) {
            throw new BusinessException("staff.employee_no_exists");
        }
        NonTeachingStaff staff = new NonTeachingStaff();
        staff.setSchoolId(schoolId);
        staff.setStatus(ACTIVE);
        apply(staff, request, employeeNo);
        ProvisionedAccount account = provisionIfNeeded(schoolId, "ADMIN", request);
        if (account != null) {
            staff.setUserId(account.userId());
        }
        staff = nonTeachingStaffRepository.save(staff);
        return account == null
                ? StaffMemberDto.from(staff)
                : StaffMemberDto.from(staff, account.username(), account.temporaryPassword());
    }

    @Transactional
    public StaffMemberDto updateNonTeachingStaff(UUID id, StaffMemberRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        NonTeachingStaff staff = findNonTeachingStaff(id, schoolId);
        String employeeNo = blankToNull(request.employeeNo());
        if (employeeNo == null) {
            employeeNo = staff.getEmployeeNo();
        } else if (!employeeNo.equals(staff.getEmployeeNo())
                && nonTeachingStaffRepository.existsBySchoolIdAndEmployeeNoAndIdNot(schoolId, employeeNo, id)) {
            throw new BusinessException("staff.employee_no_exists");
        }
        apply(staff, request, employeeNo);
        return StaffMemberDto.from(nonTeachingStaffRepository.save(staff));
    }

    @Transactional
    public void deactivateNonTeachingStaff(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        NonTeachingStaff staff = findNonTeachingStaff(id, schoolId);
        staff.setStatus(INACTIVE);
        nonTeachingStaffRepository.save(staff);
    }

    // ---- Sections (class teacher picker) ---------------------------------

    @Transactional(readOnly = true)
    public List<SectionRefDto> listSections() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UUID yearId = currentYearIdOrNull(schoolId);
        Map<UUID, String> classNames = classNameById(schoolId);
        Map<UUID, TeacherProfile> teachers = teacherById(schoolId);
        Map<UUID, TeacherSection> classTeacherBySection = new HashMap<>();
        for (TeacherSection link : teacherSectionRepository.findBySchoolId(schoolId)) {
            if (link.isClassTeacher() && inYear(link, yearId)) {
                classTeacherBySection.putIfAbsent(link.getSectionId(), link);
            }
        }
        return sectionRepository.findBySchoolIdOrderByNameAsc(schoolId).stream()
                .map(section -> {
                    TeacherSection link = classTeacherBySection.get(section.getId());
                    UUID teacherId = link == null ? null : link.getTeacherId();
                    TeacherProfile teacher = teacherId == null ? null : teachers.get(teacherId);
                    return new SectionRefDto(section.getId(), section.getClassId(),
                            classNames.get(section.getClassId()), section.getName(),
                            sectionLabel(classNames.get(section.getClassId()), section.getName()),
                            teacherId, teacher == null ? null : teacher.getDisplayName());
                })
                .toList();
    }

    // ---- Helpers ---------------------------------------------------------

    private void apply(TeacherProfile teacher, StaffMemberRequest request, String employeeNo) {
        teacher.setEmployeeNo(employeeNo);
        teacher.setFirstName(request.firstName().trim());
        teacher.setLastName(blankToNull(request.lastName()));
        teacher.setEmail(blankToNull(request.email()));
        teacher.setPhone(blankToNull(request.phone()));
        teacher.setGender(request.gender());
        teacher.setDateOfBirth(request.dateOfBirth());
        teacher.setDesignation(blankToNull(request.designation()));
        teacher.setDepartment(blankToNull(request.department()));
        teacher.setQualification(blankToNull(request.qualification()));
        teacher.setEmploymentType(request.employmentType());
        teacher.setJoinDate(request.joinDate());
        teacher.setAddress(blankToNull(request.address()));
        teacher.setUserId(request.userId());
    }

    private void apply(NonTeachingStaff staff, StaffMemberRequest request, String employeeNo) {
        staff.setEmployeeNo(employeeNo);
        staff.setFirstName(request.firstName().trim());
        staff.setLastName(blankToNull(request.lastName()));
        staff.setEmail(blankToNull(request.email()));
        staff.setPhone(blankToNull(request.phone()));
        staff.setGender(request.gender());
        staff.setDateOfBirth(request.dateOfBirth());
        staff.setDesignation(blankToNull(request.designation()));
        staff.setDepartment(blankToNull(request.department()));
        staff.setQualification(blankToNull(request.qualification()));
        staff.setEmploymentType(request.employmentType());
        staff.setJoinDate(request.joinDate());
        staff.setAddress(blankToNull(request.address()));
        staff.setUserId(request.userId());
    }

    private StaffMemberDto toDto(TeacherProfile teacher, UUID schoolId, List<SectionRefDto> classTeacherOf) {
        List<UUID> subjectIds = teacherSubjectRepository
                .findByTeacherIdAndSchoolId(teacher.getId(), schoolId).stream()
                .map(TeacherSubject::getSubjectId).toList();
        return StaffMemberDto.from(teacher, subjectIds, classTeacherOf);
    }

    private ProvisionedAccount provisionIfNeeded(UUID schoolId, String roleCode, StaffMemberRequest request) {
        if (request.userId() != null) {
            return null;
        }
        return credentialAccountService.provision(schoolId, roleCode,
                request.firstName(), request.lastName(), request.email(), request.phone());
    }

    private StaffMemberDto withCredentials(StaffMemberDto dto, ProvisionedAccount account) {
        if (account == null) {
            return dto;
        }
        return new StaffMemberDto(
                dto.id(), dto.staffType(), account.userId(), dto.employeeNo(),
                dto.firstName(), dto.lastName(), dto.displayName(), dto.email(), dto.phone(),
                dto.gender(), dto.dateOfBirth(), dto.designation(), dto.department(),
                dto.qualification(), dto.employmentType(), dto.joinDate(), dto.address(),
                dto.status(), dto.subjectIds(), dto.classTeacherOf(),
                account.username(), account.temporaryPassword());
    }

    private Map<UUID, List<SectionRefDto>> classTeacherRefsByTeacher(UUID schoolId, UUID yearId) {
        Map<UUID, String> classNames = classNameById(schoolId);
        Map<UUID, Section> sections = sectionRepository.findBySchoolIdOrderByNameAsc(schoolId).stream()
                .collect(Collectors.toMap(Section::getId, Function.identity()));
        Map<UUID, TeacherProfile> teachers = teacherById(schoolId);
        Map<UUID, List<SectionRefDto>> result = new HashMap<>();
        for (TeacherSection link : teacherSectionRepository.findBySchoolId(schoolId)) {
            if (!link.isClassTeacher() || !inYear(link, yearId)) {
                continue;
            }
            Section section = sections.get(link.getSectionId());
            if (section == null) {
                continue;
            }
            TeacherProfile teacher = teachers.get(link.getTeacherId());
            result.computeIfAbsent(link.getTeacherId(), key -> new ArrayList<>())
                    .add(new SectionRefDto(section.getId(), section.getClassId(),
                            classNames.get(section.getClassId()), section.getName(),
                            sectionLabel(classNames.get(section.getClassId()), section.getName()),
                            link.getTeacherId(), teacher == null ? null : teacher.getDisplayName()));
        }
        return result;
    }

    private List<SectionRefDto> classTeacherRefs(TeacherProfile teacher, UUID schoolId, UUID yearId) {
        return classTeacherRefsByTeacher(schoolId, yearId)
                .getOrDefault(teacher.getId(), List.of());
    }

    private void clearClassTeacher(UUID teacherId, UUID schoolId) {
        for (TeacherSection link : teacherSectionRepository.findByTeacherIdAndSchoolId(teacherId, schoolId)) {
            if (link.isClassTeacher()) {
                link.setClassTeacher(false);
                teacherSectionRepository.save(link);
            }
        }
    }

    private TeacherProfile findTeacher(UUID id, UUID schoolId) {
        return teacherRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("teacher", id));
    }

    private NonTeachingStaff findNonTeachingStaff(UUID id, UUID schoolId) {
        return nonTeachingStaffRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("staff_member", id));
    }

    private Map<UUID, String> classNameById(UUID schoolId) {
        return classRepository.findBySchoolIdOrderBySortOrderAsc(schoolId).stream()
                .collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getName, (a, b) -> a));
    }

    private Map<UUID, TeacherProfile> teacherById(UUID schoolId) {
        return teacherRepository.findBySchoolIdOrderByFirstNameAsc(schoolId).stream()
                .collect(Collectors.toMap(TeacherProfile::getId, Function.identity(), (a, b) -> a));
    }

    private UUID currentYearIdOrNull(UUID schoolId) {
        return academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId)
                .map(AcademicYear::getId).orElse(null);
    }

    private boolean inYear(TeacherSection link, UUID yearId) {
        return yearId == null || yearId.equals(link.getAcademicYearId());
    }

    private String sectionLabel(String className, String sectionName) {
        return className == null || className.isBlank() ? sectionName : className + " - " + sectionName;
    }

    private String resolveEmployeeNo(UUID schoolId, String requested, boolean teaching) {
        String employeeNo = blankToNull(requested);
        if (employeeNo != null) {
            return employeeNo;
        }
        return teaching
                ? recordIdGenerator.nextTeacherEmployeeNo(schoolId)
                : recordIdGenerator.nextNonTeachingEmployeeNo(schoolId);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
