package com.schoolms.student;

import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.SchoolClass;
import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.academics.Section;
import com.schoolms.academics.SectionRepository;
import com.schoolms.academics.StudentEnrollment;
import com.schoolms.academics.StudentEnrollmentRepository;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.common.enums.Gender;
import com.schoolms.common.enums.GuardianRelationship;
import com.schoolms.common.enums.StudentStatus;
import com.schoolms.security.SecurityUtils;
import com.schoolms.student.dto.EnrollRequest;
import com.schoolms.student.dto.GuardianDto;
import com.schoolms.student.dto.GuardianRequest;
import com.schoolms.student.dto.ImportResult;
import com.schoolms.student.dto.StudentDto;
import com.schoolms.student.dto.StudentGuardianDto;
import com.schoolms.student.dto.StudentListItemDto;
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
        if (enrollmentRepository.existsByStudentIdAndAcademicYearId(studentId, request.academicYearId())) {
            throw new BusinessException("enrollment.exists");
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
}
