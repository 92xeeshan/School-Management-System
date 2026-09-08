package com.schoolms.student;

import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.SchoolClass;
import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.academics.Section;
import com.schoolms.academics.SectionRepository;
import com.schoolms.academics.StudentEnrollment;
import com.schoolms.academics.StudentEnrollmentRepository;
import com.schoolms.academics.TeacherProfile;
import com.schoolms.academics.TeacherProfileRepository;
import com.schoolms.academics.TeacherSectionRepository;
import com.schoolms.attendance.AttendanceService;
import com.schoolms.attendance.dto.AttendanceSummaryDto;
import com.schoolms.common.exception.AuthException;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.common.enums.Gender;
import com.schoolms.common.enums.GuardianRelationship;
import com.schoolms.common.enums.StudentStatus;
import com.schoolms.security.SecurityUtils;
import com.schoolms.security.UserPrincipal;
import com.schoolms.student.dto.EnrollRequest;
import com.schoolms.student.dto.GuardianDto;
import com.schoolms.student.dto.GuardianRequest;
import com.schoolms.student.dto.ImportResult;
import com.schoolms.student.dto.StudentDto;
import com.schoolms.student.dto.StudentGuardianDto;
import com.schoolms.student.dto.StudentListItemDto;
import com.schoolms.student.dto.StudentProfileDto;
import com.schoolms.student.dto.StudentProfileUpdateRequest;
import com.schoolms.student.dto.StudentRequest;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final AcademicYearRepository academicYearRepository;
    private final TeacherProfileRepository teacherRepository;
    private final TeacherSectionRepository teacherSectionRepository;
    private final AttendanceService attendanceService;

    @Transactional(readOnly = true)
    public Page<StudentListItemDto> list(int page, int size, String query) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        Pageable pageable = PageRequest.of(page, size);
        Page<Student> students = query == null || query.isBlank()
                ? studentRepository.findBySchoolId(schoolId, pageable)
                : studentRepository.search(schoolId, query.trim(), pageable);
        return students.map(student -> toListItem(student, schoolId));
    }

    @Transactional(readOnly = true)
    public List<StudentListItemDto> listBySection(UUID sectionId, UUID academicYearId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        return studentRepository.findBySectionAndYear(schoolId, sectionId, academicYearId).stream()
                .map(student -> toListItem(student, schoolId))
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentDto get(UUID id) {
        return StudentDto.from(findStudent(id));
    }

    @Transactional(readOnly = true)
    public StudentProfileDto getProfile(UUID id) {
        Student student = findStudent(id);
        return toProfile(student);
    }

    @Transactional
    public StudentProfileDto updateProfile(UUID id, StudentProfileUpdateRequest request) {
        Student student = findStudent(id);
        ProfileAccess access = profileAccess(student);
        if (!access.canEdit()) {
            throw AuthException.accessDenied();
        }
        applyProfile(student, request, access.editableFields());
        studentRepository.save(student);
        if (access.editableFields().contains("classId") || access.editableFields().contains("sectionId")
                || access.editableFields().contains("rollNumber")) {
            applyEnrollment(student.getId(), request, access.editableFields());
        }
        upsertParentGuardians(student, request, access.editableFields());
        return toProfile(student);
    }

    @Transactional(readOnly = true)
    public List<StudentGuardianDto> guardiansOf(UUID studentId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        findStudent(studentId);
        return studentGuardianRepository.findWithGuardians(schoolId, studentId).stream()
                .map(sg -> new StudentGuardianDto(sg.getId(), GuardianDto.from(sg.getGuardian()),
                        sg.getRelationship().name(), sg.isPrimary()))
                .toList();
    }

    @Transactional
    public StudentDto create(StudentRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        if (studentRepository.existsBySchoolIdAndAdmissionNo(schoolId, request.admissionNo())) {
            throw new BusinessException("student.admission_exists");
        }
        Student student = new Student();
        apply(student, request);
        student.setSchoolId(schoolId);
        return StudentDto.from(studentRepository.save(student));
    }

    @Transactional
    public StudentDto update(UUID id, StudentRequest request) {
        Student student = findStudent(id);
        apply(student, request);
        return StudentDto.from(studentRepository.save(student));
    }

    @Transactional
    public void deactivate(UUID id) {
        Student student = findStudent(id);
        student.setStatus(StudentStatus.INACTIVE);
        studentRepository.save(student);
    }

    @Transactional
    public StudentEnrollment enroll(UUID studentId, EnrollRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        findStudent(studentId);
        Section section = sectionRepository.findByIdAndSchoolId(request.sectionId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("section", request.sectionId()));
        if (!section.getClassId().equals(request.classId())) {
            throw new BusinessException("enrollment.not_found");
        }
        academicYearRepository.findByIdAndSchoolId(request.academicYearId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_year", request.academicYearId()));
        Optional<StudentEnrollment> existing = enrollmentRepository
                .findByStudentIdAndAcademicYearId(studentId, request.academicYearId());
        if (existing.isPresent()) {
            StudentEnrollment enrollment = existing.get();
            enrollment.setSectionId(request.sectionId());
            if (request.rollNumber() != null) {
                enrollment.setRollNumber(request.rollNumber());
            }
            enrollment.setStatus("ACTIVE");
            return enrollmentRepository.save(enrollment);
        }
        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setSchoolId(schoolId);
        enrollment.setStudentId(studentId);
        enrollment.setSectionId(request.sectionId());
        enrollment.setAcademicYearId(request.academicYearId());
        enrollment.setRollNumber(request.rollNumber());
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setStatus("ACTIVE");
        return enrollmentRepository.save(enrollment);
    }

    @Transactional
    public StudentGuardianDto assignGuardian(UUID studentId, UUID guardianId,
                                             String relationship, boolean primary) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        findStudent(studentId);
        Guardian guardian = guardianRepository.findByIdAndSchoolId(guardianId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("guardian", guardianId));
        if (studentGuardianRepository.existsByStudentIdAndGuardianId(studentId, guardianId)) {
            throw new BusinessException("guardian.not_found");
        }
        StudentGuardian link = new StudentGuardian();
        link.setSchoolId(schoolId);
        link.setStudent(studentRepository.getReferenceById(studentId));
        link.setGuardian(guardian);
        link.setRelationship(parseRelationship(relationship));
        link.setPrimary(primary);
        if (primary) {
            clearPrimaryFlags(studentId, schoolId);
        }
        return toGuardianDto(studentGuardianRepository.save(link));
    }

    @Transactional
    public void unlinkGuardian(UUID studentId, UUID guardianId) {
        studentGuardianRepository.findByStudentIdAndGuardianId(studentId, guardianId)
                .ifPresent(studentGuardianRepository::delete);
    }

    @Transactional
    public StudentGuardianDto createAndLinkGuardian(UUID studentId, GuardianRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        findStudent(studentId);
        Guardian guardian = new Guardian();
        guardian.setSchoolId(schoolId);
        guardian.setFirstName(request.firstName());
        guardian.setLastName(request.lastName());
        guardian.setRelationship(parseRelationship(request.relationship()));
        guardian.setEmail(request.email());
        guardian.setPhone(request.phone());
        guardian.setOccupation(request.occupation());
        guardian.setUserId(request.userId());
        guardian = guardianRepository.save(guardian);

        StudentGuardian link = new StudentGuardian();
        link.setSchoolId(schoolId);
        link.setStudent(studentRepository.getReferenceById(studentId));
        link.setGuardian(guardian);
        link.setRelationship(parseRelationship(request.relationship()));
        link.setPrimary(true);
        clearPrimaryFlags(studentId, schoolId);
        return toGuardianDto(studentGuardianRepository.save(link));
    }

    @Transactional
    public ImportResult importCsv(MultipartFile file) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        List<String> errors = new ArrayList<>();
        int total = 0;
        int succeeded = 0;

        Map<String, AcademicYear> yearsByName = academicYearRepository
                .findBySchoolIdOrderByStartDateDesc(schoolId).stream()
                .collect(Collectors.toMap(AcademicYear::getName, Function.identity()));
        Map<String, SchoolClass> classesByName = classRepository
                .findBySchoolIdOrderBySortOrderAsc(schoolId).stream()
                .collect(Collectors.toMap(SchoolClass::getName, Function.identity()));
        Map<String, Section> sectionsByName = sectionRepository.findBySchoolIdOrderByNameAsc(schoolId).stream()
                .collect(Collectors.toMap(s -> s.getClassId() + "|" + s.getName(), Function.identity()));

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))
                .withSkipLines(1).build()) {
            String[] row;
            try {
                while ((row = reader.readNext()) != null) {
                    if (row.length < 4 || row[0] == null || row[0].isBlank()) {
                        continue;
                    }
                    total++;
                    try {
                        importRow(schoolId, row, yearsByName, classesByName, sectionsByName);
                        succeeded++;
                    } catch (Exception e) {
                        errors.add("Row " + total + ": " + e.getMessage());
                    }
                }
            } catch (CsvValidationException e) {
                throw new BusinessException("student.import_failed");
            }
        } catch (IOException e) {
            throw new BusinessException("student.import_failed");
        }
        return new ImportResult(total, succeeded, errors);
    }

    private void importRow(UUID schoolId, String[] row,
                           Map<String, AcademicYear> yearsByName,
                           Map<String, SchoolClass> classesByName,
                           Map<String, Section> sectionsByName) {
        String admissionNo = row[0].trim();
        String firstName = row[1].trim();
        String lastName = row.length > 2 ? row[2] : null;
        LocalDate dob = row.length > 3 && !row[3].isBlank() ? LocalDate.parse(row[3].trim()) : null;
        String className = row.length > 4 ? row[4] : null;
        String sectionName = row.length > 5 ? row[5] : null;

        if (lastName != null) {
            lastName = lastName.trim();
            if (lastName.isBlank()) {
                lastName = null;
            }
        }

        Student student = new Student();
        student.setSchoolId(schoolId);
        student.setAdmissionNo(admissionNo);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setDateOfBirth(dob);
        student.setGender(parseGender(row.length > 6 ? row[6] : null));
        student.setAdmissionDate(LocalDate.now());
        studentRepository.save(student);

        if (className != null && sectionName != null) {
            SchoolClass schoolClass = classesByName.get(className.trim());
            Section section = sectionsByName.get(schoolClass.getId() + "|" + sectionName.trim());
            AcademicYear year = yearsByName.values().stream()
                    .filter(AcademicYear::isCurrent).findFirst().orElse(null);
            if (year != null) {
                StudentEnrollment enrollment = new StudentEnrollment();
                enrollment.setSchoolId(schoolId);
                enrollment.setStudentId(student.getId());
                enrollment.setSectionId(section.getId());
                enrollment.setAcademicYearId(year.getId());
                enrollment.setRollNumber(row.length > 7 && !row[7].isBlank()
                        ? Integer.parseInt(row[7].trim()) : null);
                enrollment.setEnrollmentDate(LocalDate.now());
                enrollment.setStatus("ACTIVE");
                enrollmentRepository.save(enrollment);
            }
        }
    }

    private void apply(Student student, StudentRequest request) {
        student.setAdmissionNo(request.admissionNo());
        student.setFirstName(request.firstName());
        student.setLastName(request.lastName());
        student.setDateOfBirth(request.dateOfBirth());
        student.setGender(parseGender(request.gender()));
        student.setBloodGroup(request.bloodGroup());
        student.setReligion(request.religion());
        student.setNationality(request.nationality());
        student.setAdmissionDate(request.admissionDate() == null ? LocalDate.now() : request.admissionDate());
        student.setPhone(request.phone());
        student.setEmergencyContact(request.emergencyContact());
        student.setPermanentAddress(request.permanentAddress());
        student.setPresentAddress(request.presentAddress());
        student.setPreviousSchool(request.previousSchool());
        student.setUserId(request.userId());
    }

    private Student findStudent(UUID id) {
        return studentRepository.findByIdAndSchoolId(id, SecurityUtils.currentSchoolId())
                .orElseThrow(() -> ResourceNotFoundException.of("student", id));
    }

    private Gender parseGender(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Gender.valueOf(value.trim().toUpperCase());
    }

    private GuardianRelationship parseRelationship(String value) {
        if (value == null || value.isBlank()) {
            return GuardianRelationship.GUARDIAN;
        }
        return GuardianRelationship.valueOf(value.trim().toUpperCase());
    }

    private void clearPrimaryFlags(UUID studentId, UUID schoolId) {
        for (StudentGuardian link : studentGuardianRepository.findByStudentIdAndSchoolId(studentId, schoolId)) {
            if (link.isPrimary()) {
                link.setPrimary(false);
                studentGuardianRepository.save(link);
            }
        }
    }

    private StudentGuardianDto toGuardianDto(StudentGuardian link) {
        return new StudentGuardianDto(link.getId(), GuardianDto.from(link.getGuardian()),
                link.getRelationship().name(), link.isPrimary());
    }

    private StudentListItemDto toListItem(Student student, UUID schoolId) {
        Optional<StudentEnrollment> enrollment = enrollmentRepository
                .findByStudentIdAndAcademicYearId(student.getId(), currentYearId(schoolId));
        String className = null;
        String sectionName = null;
        UUID enrollmentId = null;
        UUID academicYearId = null;
        Integer rollNumber = null;
        if (enrollment.isPresent()) {
            StudentEnrollment e = enrollment.get();
            enrollmentId = e.getId();
            academicYearId = e.getAcademicYearId();
            rollNumber = e.getRollNumber();
            Section section = sectionRepository.findByIdAndSchoolId(e.getSectionId(), schoolId).orElse(null);
            if (section != null) {
                sectionName = section.getName();
                className = classRepository.findByIdAndSchoolId(section.getClassId(), schoolId)
                        .map(SchoolClass::getName).orElse(null);
            }
        }
        return new StudentListItemDto(StudentDto.from(student), enrollmentId, academicYearId,
                className, sectionName, rollNumber);
    }

    private UUID currentYearId(UUID schoolId) {
        return academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId)
                .map(AcademicYear::getId).orElse(null);
    }

    private StudentProfileDto toProfile(Student student) {
        UUID schoolId = student.getSchoolId();
        EnrollmentView enrollment = currentEnrollment(student.getId(), schoolId);
        GuardianNames parents = parentNames(student.getId(), schoolId);
        ProfileAccess access = profileAccess(student, enrollment.sectionId());
        AttendanceSummaryDto attendance = null;
        AcademicYear year = enrollment.academicYearId() == null
                ? academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId).orElse(null)
                : academicYearRepository.findByIdAndSchoolId(enrollment.academicYearId(), schoolId).orElse(null);
        if (year != null) {
            attendance = attendanceService.getStudentSummary(student.getId(), year.getStartDate(), year.getEndDate());
        }
        return new StudentProfileDto(
                student.getId(), student.getAdmissionNo(), student.getFirstName(), student.getLastName(),
                student.getDisplayName(), student.getDateOfBirth(), student.getGender(), student.getBloodGroup(),
                student.getReligion(), student.getNationality(), student.getPhone(), student.getEmergencyContact(),
                student.getPermanentAddress(), student.getPresentAddress(), student.getPreviousSchool(),
                parents.fatherName(), parents.motherName(), parents.fatherPhone(), parents.motherPhone(),
                student.getAdmissionDate(), student.getPhotoUrl(), student.getStatus(),
                enrollment.classId(), enrollment.className(), enrollment.sectionId(), enrollment.sectionName(),
                enrollment.rollNumber(), enrollment.academicYearId(), enrollment.academicYearName(),
                enrollment.enrollmentDate(), attendance, access.canEdit(), access.editableFields());
    }

    private ProfileAccess profileAccess(Student student) {
        return profileAccess(student, currentEnrollment(student.getId(), student.getSchoolId()).sectionId());
    }

    private ProfileAccess profileAccess(Student student, UUID sectionId) {
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        if (principal.hasRole("ADMIN") || principal.hasRole("SUPER_ADMIN")) {
            return new ProfileAccess(true, List.of(
                    "firstName", "lastName", "dateOfBirth", "gender", "bloodGroup", "religion", "nationality",
                    "phone", "emergencyContact", "permanentAddress", "presentAddress", "previousSchool",
                    "fatherName", "motherName", "fatherPhone", "motherPhone", "admissionDate",
                    "classId", "sectionId", "rollNumber"));
        }
        if (principal.hasRole("TEACHER") && isClassTeacherOf(principal.id(), sectionId, student.getSchoolId())) {
            return new ProfileAccess(true, List.of(
                    "firstName", "lastName", "dateOfBirth", "phone", "emergencyContact",
                    "fatherName", "motherName", "fatherPhone", "motherPhone", "rollNumber"));
        }
        return new ProfileAccess(false, List.of());
    }

    private boolean isClassTeacherOf(UUID userId, UUID sectionId, UUID schoolId) {
        if (sectionId == null) {
            return false;
        }
        TeacherProfile teacher = teacherRepository.findBySchoolIdAndUserId(schoolId, userId).orElse(null);
        if (teacher == null) {
            return false;
        }
        UUID yearId = currentYearId(schoolId);
        return teacherSectionRepository.findByTeacherIdAndSchoolId(teacher.getId(), schoolId).stream()
                .anyMatch(assignment -> assignment.getSectionId().equals(sectionId)
                        && assignment.isClassTeacher()
                        && (yearId == null || yearId.equals(assignment.getAcademicYearId())));
    }

    private void applyProfile(Student student, StudentProfileUpdateRequest request, List<String> fields) {
        if (fields.contains("firstName")) {
            student.setFirstName(request.firstName());
        }
        if (fields.contains("lastName")) {
            student.setLastName(request.lastName());
        }
        if (fields.contains("dateOfBirth")) {
            student.setDateOfBirth(request.dateOfBirth());
        }
        if (fields.contains("gender")) {
            student.setGender(parseGender(request.gender()));
        }
        if (fields.contains("bloodGroup")) {
            student.setBloodGroup(request.bloodGroup());
        }
        if (fields.contains("religion")) {
            student.setReligion(request.religion());
        }
        if (fields.contains("nationality")) {
            student.setNationality(request.nationality());
        }
        if (fields.contains("phone")) {
            student.setPhone(request.phone());
        }
        if (fields.contains("emergencyContact")) {
            student.setEmergencyContact(request.emergencyContact());
        }
        if (fields.contains("permanentAddress")) {
            student.setPermanentAddress(request.permanentAddress());
        }
        if (fields.contains("presentAddress")) {
            student.setPresentAddress(request.presentAddress());
        }
        if (fields.contains("previousSchool")) {
            student.setPreviousSchool(request.previousSchool());
        }
        if (fields.contains("admissionDate") && request.admissionDate() != null) {
            student.setAdmissionDate(request.admissionDate());
        }
    }

    private void applyEnrollment(UUID studentId, StudentProfileUpdateRequest request, List<String> fields) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UUID yearId = currentYearId(schoolId);
        if (yearId == null) {
            return;
        }
        Optional<StudentEnrollment> existing = enrollmentRepository.findByStudentIdAndAcademicYearId(studentId, yearId);
        if (fields.contains("sectionId") && request.sectionId() != null && request.classId() != null) {
            enroll(studentId, new EnrollRequest(request.classId(), request.sectionId(), yearId, request.rollNumber()));
            return;
        }
        if (fields.contains("rollNumber") && request.rollNumber() != null && existing.isPresent()) {
            StudentEnrollment enrollment = existing.get();
            enrollment.setRollNumber(request.rollNumber());
            enrollmentRepository.save(enrollment);
        }
    }

    private void upsertParentGuardians(Student student, StudentProfileUpdateRequest request, List<String> fields) {
        if (!fields.contains("fatherName") && !fields.contains("motherName")
                && !fields.contains("fatherPhone") && !fields.contains("motherPhone")) {
            return;
        }
        UUID schoolId = student.getSchoolId();
        List<StudentGuardian> links = studentGuardianRepository.findWithGuardians(schoolId, student.getId());
        if (fields.contains("fatherName") || fields.contains("fatherPhone")) {
            upsertParent(student, links, GuardianRelationship.FATHER, request.fatherName(), request.fatherPhone());
        }
        if (fields.contains("motherName") || fields.contains("motherPhone")) {
            upsertParent(student, links, GuardianRelationship.MOTHER, request.motherName(), request.motherPhone());
        }
    }

    private void upsertParent(Student student, List<StudentGuardian> links,
                              GuardianRelationship relationship, String fullName, String phone) {
        StudentGuardian existing = links.stream()
                .filter(link -> link.getRelationship() == relationship)
                .findFirst()
                .orElse(null);
        String trimmed = fullName == null ? "" : fullName.trim();
        if (trimmed.isBlank() && (phone == null || phone.isBlank()) && existing == null) {
            return;
        }
        String firstName = trimmed;
        String lastName = null;
        int space = trimmed.lastIndexOf(' ');
        if (space > 0) {
            firstName = trimmed.substring(0, space).trim();
            lastName = trimmed.substring(space + 1).trim();
        }
        if (existing == null) {
            if (firstName.isBlank()) {
                return;
            }
            Guardian guardian = new Guardian();
            guardian.setSchoolId(student.getSchoolId());
            guardian.setFirstName(firstName);
            guardian.setLastName(lastName);
            guardian.setRelationship(relationship);
            guardian.setPhone(phone);
            guardian = guardianRepository.save(guardian);
            StudentGuardian link = new StudentGuardian();
            link.setSchoolId(student.getSchoolId());
            link.setStudent(student);
            link.setGuardian(guardian);
            link.setRelationship(relationship);
            link.setPrimary(relationship == GuardianRelationship.FATHER);
            studentGuardianRepository.save(link);
            return;
        }
        Guardian guardian = existing.getGuardian();
        if (!firstName.isBlank()) {
            guardian.setFirstName(firstName);
            guardian.setLastName(lastName);
        }
        if (phone != null) {
            guardian.setPhone(phone);
        }
        guardianRepository.save(guardian);
    }

    private GuardianNames parentNames(UUID studentId, UUID schoolId) {
        String fatherName = null;
        String motherName = null;
        String fatherPhone = null;
        String motherPhone = null;
        for (StudentGuardian link : studentGuardianRepository.findWithGuardians(schoolId, studentId)) {
            Guardian guardian = link.getGuardian();
            if (guardian == null) {
                continue;
            }
            if (link.getRelationship() == GuardianRelationship.FATHER) {
                fatherName = guardian.getDisplayName();
                fatherPhone = guardian.getPhone();
            } else if (link.getRelationship() == GuardianRelationship.MOTHER) {
                motherName = guardian.getDisplayName();
                motherPhone = guardian.getPhone();
            }
        }
        return new GuardianNames(fatherName, motherName, fatherPhone, motherPhone);
    }

    private EnrollmentView currentEnrollment(UUID studentId, UUID schoolId) {
        UUID yearId = currentYearId(schoolId);
        if (yearId == null) {
            return EnrollmentView.empty();
        }
        Optional<StudentEnrollment> enrollment = enrollmentRepository.findByStudentIdAndAcademicYearId(studentId, yearId);
        if (enrollment.isEmpty()) {
            return EnrollmentView.empty();
        }
        StudentEnrollment e = enrollment.get();
        AcademicYear year = academicYearRepository.findByIdAndSchoolId(e.getAcademicYearId(), schoolId).orElse(null);
        Section section = sectionRepository.findByIdAndSchoolId(e.getSectionId(), schoolId).orElse(null);
        UUID classId = null;
        String className = null;
        String sectionName = null;
        if (section != null) {
            classId = section.getClassId();
            sectionName = section.getName();
            className = classRepository.findByIdAndSchoolId(section.getClassId(), schoolId)
                    .map(SchoolClass::getName).orElse(null);
        }
        return new EnrollmentView(classId, className, e.getSectionId(), sectionName, e.getRollNumber(),
                e.getAcademicYearId(), year == null ? null : year.getName(), e.getEnrollmentDate());
    }

    private record ProfileAccess(boolean canEdit, List<String> editableFields) {
    }

    private record GuardianNames(String fatherName, String motherName, String fatherPhone, String motherPhone) {
    }

    private record EnrollmentView(UUID classId, String className, UUID sectionId, String sectionName,
                                  Integer rollNumber, UUID academicYearId, String academicYearName,
                                  LocalDate enrollmentDate) {
        static EnrollmentView empty() {
            return new EnrollmentView(null, null, null, null, null, null, null, null);
        }
    }
}
