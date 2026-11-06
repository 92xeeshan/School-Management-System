package com.schoolms.student;

import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.ClassSubject;
import com.schoolms.academics.ClassSubjectRepository;
import com.schoolms.academics.SchoolClass;
import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.academics.SchoolLookup;
import com.schoolms.academics.SchoolLookupRepository;
import com.schoolms.academics.Section;
import com.schoolms.academics.SectionRepository;
import com.schoolms.academics.Stream;
import com.schoolms.academics.StreamRepository;
import com.schoolms.academics.StudentEnrollment;
import com.schoolms.academics.StudentEnrollmentRepository;
import com.schoolms.academics.Subject;
import com.schoolms.academics.SubjectRepository;
import com.schoolms.academics.dto.ClassSubjectOptionDto;
import com.schoolms.academics.dto.LookupDto;
import com.schoolms.academics.dto.LookupRequest;
import com.schoolms.academics.dto.StreamDto;
import com.schoolms.academics.dto.StreamRequest;
import com.schoolms.auth.CredentialAccountService;
import com.schoolms.common.RecordIdGenerator;
import com.schoolms.common.enums.AddressType;
import com.schoolms.common.enums.AdmissionStepStatus;
import com.schoolms.common.enums.Gender;
import com.schoolms.common.enums.GuardianRelationship;
import com.schoolms.common.enums.LookupKind;
import com.schoolms.common.enums.StudentDocumentType;
import com.schoolms.common.enums.StudentStatus;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.file.MinioService;
import com.schoolms.security.SecurityUtils;
import com.schoolms.student.dto.AddressDto;
import com.schoolms.student.dto.AdmissionStep1Request;
import com.schoolms.student.dto.AdmissionStep2Request;
import com.schoolms.student.dto.AdmissionStep3Request;
import com.schoolms.student.dto.AdmissionStep4Request;
import com.schoolms.student.dto.AdmissionStep5Request;
import com.schoolms.student.dto.AdmissionStep6Request;
import com.schoolms.student.dto.AdmissionWizardDto;
import com.schoolms.student.dto.EnrollRequest;
import com.schoolms.student.dto.FamilyMemberDto;
import com.schoolms.student.dto.PreviousSchoolDto;
import com.schoolms.student.dto.SiblingDto;
import com.schoolms.student.dto.StepStatusDto;
import com.schoolms.student.dto.StudentDocumentDto;
import com.schoolms.user.dto.ProvisionedAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdmissionService {

    private static final Set<String> PHOTO_TYPES = Set.of("image/jpeg", "image/png", "image/jpg");
    private static final Set<String> DOC_TYPES = Set.of("image/jpeg", "image/png", "image/jpg", "application/pdf");
    private static final long PHOTO_MAX = 2 * 1024 * 1024;
    private static final long DOC_MAX = 5 * 1024 * 1024;

    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentStepStatusRepository stepStatusRepository;
    private final StudentAddressRepository addressRepository;
    private final StudentPreviousSchoolRepository previousSchoolRepository;
    private final StudentDocumentRepository documentRepository;
    private final StudentSubjectRepository studentSubjectRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final AcademicYearRepository academicYearRepository;
    private final StreamRepository streamRepository;
    private final SchoolLookupRepository lookupRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final StudentService studentService;
    private final CredentialAccountService credentialAccountService;
    private final RecordIdGenerator recordIdGenerator;
    private final MinioService minioService;

    @Transactional
    public AdmissionWizardDto create(AdmissionStep1Request request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        validateIds(request);
        String admissionNo = recordIdGenerator.nextAdmissionNo(schoolId);
        if (studentRepository.existsBySchoolIdAndAdmissionNo(schoolId, admissionNo)) {
            throw new BusinessException("student.admission_exists");
        }
        assertUniqueIds(schoolId, null, request.aadhaarNo(), request.apaarId());

        Student student = new Student();
        student.setSchoolId(schoolId);
        student.setAdmissionNo(admissionNo);
        student.setAdmissionDate(LocalDate.now());
        student.setStatus(StudentStatus.ACTIVE);
        student.setProfilePending(true);
        applyStep1(student, request);
        ProvisionedAccount account = credentialAccountService.provision(schoolId, "STUDENT",
                request.firstName(), request.lastName(), null, null);
        student.setUserId(account.userId());
        student = studentRepository.save(student);
        enroll(student, request.classId(), request.sectionId(), resolveYearId(schoolId, request.academicYearId()), null);
        initSteps(student);
        markStep(student, 1, AdmissionStepStatus.COMPLETED);
        return toWizard(student, account.username(), account.temporaryPassword());
    }

    @Transactional
    public AdmissionWizardDto saveStep(UUID studentId, int stepNo, Object body) {
        Student student = findStudent(studentId);
        return switch (stepNo) {
            case 1 -> saveStep1(student, (AdmissionStep1Request) body);
            case 2 -> saveStep2(student, (AdmissionStep2Request) body);
            case 3 -> saveStep3(student, (AdmissionStep3Request) body);
            case 4 -> saveStep4(student, (AdmissionStep4Request) body);
            case 5 -> saveStep5(student, (AdmissionStep5Request) body);
            case 6 -> saveStep6(student, (AdmissionStep6Request) body);
            default -> throw new BusinessException("student.invalid_step");
        };
    }

    @Transactional(readOnly = true)
    public AdmissionWizardDto get(UUID studentId) {
        return toWizard(findStudent(studentId), null, null);
    }

    @Transactional
    public AdmissionWizardDto uploadPhoto(UUID studentId, MultipartFile file) {
        Student student = findStudent(studentId);
        validateUpload(file, PHOTO_TYPES, PHOTO_MAX, "student.photo_invalid");
        if (student.getPhotoUrl() != null) {
            minioService.delete(student.getPhotoUrl());
        }
        student.setPhotoUrl(minioService.upload(student.getSchoolId(), "student-photos", file));
        studentRepository.save(student);
        return toWizard(student, null, null);
    }

    @Transactional
    public StudentDocumentDto uploadDocument(UUID studentId, StudentDocumentType type, MultipartFile file) {
        Student student = findStudent(studentId);
        validateUpload(file, DOC_TYPES, DOC_MAX, "student.document_invalid");
        StudentDocument doc = new StudentDocument();
        doc.setSchoolId(student.getSchoolId());
        doc.setStudentId(student.getId());
        doc.setDocType(type);
        doc.setFileKey(minioService.upload(student.getSchoolId(), "student-docs", file));
        doc.setFileName(file.getOriginalFilename() == null ? "document" : file.getOriginalFilename());
        doc.setContentType(file.getContentType());
        doc.setUploadedBy(SecurityUtils.currentUserId());
        doc = documentRepository.save(doc);
        return StudentDocumentDto.from(doc, minioService.presignedUrl(doc.getFileKey()));
    }

    @Transactional
    public void deleteDocument(UUID studentId, UUID docId) {
        findStudent(studentId);
        StudentDocument doc = documentRepository.findByIdAndSchoolId(docId, SecurityUtils.currentSchoolId())
                .orElseThrow(() -> ResourceNotFoundException.of("student_document", docId));
        if (!doc.getStudentId().equals(studentId)) {
            throw ResourceNotFoundException.of("student_document", docId);
        }
        minioService.delete(doc.getFileKey());
        documentRepository.delete(doc);
    }

    @Transactional(readOnly = true)
    public List<ClassSubjectOptionDto> classSubjects(UUID classId, UUID streamId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        SchoolClass schoolClass = classRepository.findByIdAndSchoolId(classId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("school_class", classId));
        Map<UUID, Subject> subjects = subjectRepository.findBySchoolIdOrderByNameAsc(schoolId).stream()
                .collect(Collectors.toMap(Subject::getId, s -> s));
        List<ClassSubjectOptionDto> result = new ArrayList<>();
        for (ClassSubject link : classSubjectRepository.findByClassIdAndSchoolId(classId, schoolId)) {
            if (schoolClass.isStreamEnabled()) {
                if (link.getStreamId() != null && (streamId == null || !link.getStreamId().equals(streamId))) {
                    continue;
                }
            }
            Subject subject = subjects.get(link.getSubjectId());
            if (subject == null || !"ACTIVE".equals(subject.getStatus())) {
                continue;
            }
            result.add(new ClassSubjectOptionDto(subject.getId(), subject.getName(), subject.getCode(),
                    link.isCompulsory(), link.getStreamId()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<StreamDto> listStreams() {
        return streamRepository.findBySchoolIdOrderBySortOrderAsc(SecurityUtils.currentSchoolId())
                .stream().map(StreamDto::from).toList();
    }

    @Transactional
    public StreamDto createStream(StreamRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        String name = request.name().trim();
        if (streamRepository.existsBySchoolIdAndName(schoolId, name)) {
            throw new BusinessException("stream.name_exists");
        }
        Stream stream = new Stream();
        stream.setSchoolId(schoolId);
        applyStream(stream, request, name);
        return StreamDto.from(streamRepository.save(stream));
    }

    @Transactional
    public StreamDto updateStream(UUID id, StreamRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        Stream stream = streamRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("stream", id));
        String name = request.name().trim();
        if (streamRepository.existsBySchoolIdAndNameAndIdNot(schoolId, name, id)) {
            throw new BusinessException("stream.name_exists");
        }
        applyStream(stream, request, name);
        return StreamDto.from(streamRepository.save(stream));
    }

    @Transactional(readOnly = true)
    public List<LookupDto> listLookups(LookupKind kind) {
        return lookupRepository.findBySchoolIdAndKindOrderBySortOrderAsc(SecurityUtils.currentSchoolId(), kind)
                .stream().map(LookupDto::from).toList();
    }

    @Transactional
    public LookupDto createLookup(LookupKind kind, LookupRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        String name = request.name().trim();
        if (lookupRepository.existsBySchoolIdAndKindAndName(schoolId, kind, name)) {
            throw new BusinessException("lookup.name_exists");
        }
        SchoolLookup lookup = new SchoolLookup();
        lookup.setSchoolId(schoolId);
        lookup.setKind(kind);
        applyLookup(lookup, request, name);
        return LookupDto.from(lookupRepository.save(lookup));
    }

    @Transactional
    public LookupDto updateLookup(UUID id, LookupRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        SchoolLookup lookup = lookupRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("lookup", id));
        String name = request.name().trim();
        if (lookupRepository.existsBySchoolIdAndKindAndNameAndIdNot(schoolId, lookup.getKind(), name, id)) {
            throw new BusinessException("lookup.name_exists");
        }
        applyLookup(lookup, request, name);
        return LookupDto.from(lookupRepository.save(lookup));
    }

    @Transactional(readOnly = true)
    public List<SiblingDto> searchSiblings(String query) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) {
            return List.of();
        }
        return studentRepository.search(schoolId, q, org.springframework.data.domain.PageRequest.of(0, 10))
                .getContent().stream()
                .map(this::toSibling)
                .toList();
    }

    private AdmissionWizardDto saveStep1(Student student, AdmissionStep1Request request) {
        validateIds(request);
        assertUniqueIds(student.getSchoolId(), student.getId(), request.aadhaarNo(), request.apaarId());
        applyStep1(student, request);
        studentRepository.save(student);
        enroll(student, request.classId(), request.sectionId(),
                resolveYearId(student.getSchoolId(), request.academicYearId()), null);
        markStep(student, 1, AdmissionStepStatus.COMPLETED);
        return toWizard(student, null, null);
    }

    private AdmissionWizardDto saveStep2(Student student, AdmissionStep2Request request) {
        upsertFamily(student, GuardianRelationship.FATHER, request.father());
        upsertFamily(student, GuardianRelationship.MOTHER, request.mother());
        if (request.guardianDifferent() && hasName(request.guardian())) {
            upsertFamily(student, GuardianRelationship.GUARDIAN, request.guardian());
        }
        student.setEmergencyName(blankToNull(request.emergencyName()));
        student.setEmergencyRelation(blankToNull(request.emergencyRelation()));
        student.setEmergencyContact(blankToNull(request.emergencyPhone()));
        applyPrimary(student, request.primaryContact());
        studentRepository.save(student);
        markStep(student, 2, AdmissionStepStatus.COMPLETED);
        return toWizard(student, null, null);
    }

    private AdmissionWizardDto saveStep3(Student student, AdmissionStep3Request request) {
        upsertAddress(student, AddressType.CURRENT, request.currentAddress());
        AddressDto permanent = request.sameAsCurrent() ? request.currentAddress() : request.permanentAddress();
        if (permanent != null) {
            upsertAddress(student, AddressType.PERMANENT, permanent);
        }
        student.setPhone(blankToNull(request.phone()));
        student.setEmail(blankToNull(request.email()));
        student.setPresentAddress(formatAddress(request.currentAddress()));
        student.setPermanentAddress(formatAddress(permanent));
        studentRepository.save(student);
        markStep(student, 3, AdmissionStepStatus.COMPLETED);
        return toWizard(student, null, null);
    }

    private AdmissionWizardDto saveStep4(Student student, AdmissionStep4Request request) {
        UUID schoolId = student.getSchoolId();
        UUID yearId = request.academicYearId();
        academicYearRepository.findByIdAndSchoolId(yearId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_year", yearId));
        StudentEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndAcademicYearId(student.getId(), yearId)
                .orElseThrow(() -> new BusinessException("enrollment.not_found"));
        if (request.rollNumber() != null) {
            if (enrollmentRepository.existsBySectionIdAndAcademicYearIdAndRollNumberAndStatusAndStudentIdNot(
                    enrollment.getSectionId(), yearId, request.rollNumber(), "ACTIVE", student.getId())) {
                throw new BusinessException("student.roll_exists");
            }
            enrollment.setRollNumber(request.rollNumber());
            enrollmentRepository.save(enrollment);
        }
        if (request.admissionDate() != null) {
            student.setAdmissionDate(request.admissionDate());
        }
        student.setHouseId(request.houseId());
        student.setMediumId(request.mediumId());
        SchoolClass schoolClass = classOf(enrollment, schoolId);
        if (schoolClass.isStreamEnabled()) {
            student.setStreamId(request.streamId());
        } else {
            student.setStreamId(null);
        }
        replaceSubjects(student, yearId, request.subjectIds(), schoolClass, student.getStreamId());
        studentRepository.save(student);
        markStep(student, 4, AdmissionStepStatus.COMPLETED);
        return toWizard(student, null, null);
    }

    private AdmissionWizardDto saveStep5(Student student, AdmissionStep5Request request) {
        PreviousSchoolDto dto = request.previousSchool() == null
                ? new PreviousSchoolDto(true, null, null, null, null, null, null, null, null, null)
                : request.previousSchool();
        StudentPreviousSchool row = previousSchoolRepository
                .findByStudentIdAndSchoolId(student.getId(), student.getSchoolId())
                .orElseGet(StudentPreviousSchool::new);
        row.setSchoolId(student.getSchoolId());
        row.setStudentId(student.getId());
        row.setNoPreviousSchool(dto.noPreviousSchool());
        if (dto.noPreviousSchool()) {
            row.setSchoolName(null);
            row.setBoard(null);
            row.setMedium(null);
            row.setLastClass(null);
            row.setYearOfPassing(null);
            row.setPercentage(null);
            row.setTcNumber(null);
            row.setTcDate(null);
            row.setReason(null);
            student.setPreviousSchool(null);
        } else {
            row.setSchoolName(blankToNull(dto.schoolName()));
            row.setBoard(blankToNull(dto.board()));
            row.setMedium(blankToNull(dto.medium()));
            row.setLastClass(blankToNull(dto.lastClass()));
            row.setYearOfPassing(blankToNull(dto.yearOfPassing()));
            row.setPercentage(blankToNull(dto.percentage()));
            row.setTcNumber(blankToNull(dto.tcNumber()));
            row.setTcDate(dto.tcDate());
            row.setReason(blankToNull(dto.reason()));
            student.setPreviousSchool(row.getSchoolName());
        }
        previousSchoolRepository.save(row);
        studentRepository.save(student);
        markStep(student, 5, AdmissionStepStatus.COMPLETED);
        return toWizard(student, null, null);
    }

    private AdmissionWizardDto saveStep6(Student student, AdmissionStep6Request request) {
        student.setTransportRequired(request.transportRequired());
        student.setHostelRequired(request.hostelRequired());
        student.setMedicalNotes(blankToNull(request.medicalNotes()));
        student.setRemarks(blankToNull(request.remarks()));
        if (request.siblingIds() != null) {
            linkSiblings(student, request.siblingIds());
        }
        studentRepository.save(student);
        markStep(student, 6, AdmissionStepStatus.COMPLETED);
        return toWizard(student, null, null);
    }

    private void applyStep1(Student student, AdmissionStep1Request request) {
        student.setFirstName(request.firstName().trim());
        student.setMiddleName(blankToNull(request.middleName()));
        student.setLastName(blankToNull(request.lastName()));
        student.setDateOfBirth(request.dateOfBirth());
        student.setGender(Gender.valueOf(request.gender().trim().toUpperCase(Locale.ROOT)));
        student.setBloodGroup(blankToNull(request.bloodGroup()));
        student.setReligionId(request.religionId());
        student.setCategoryId(request.categoryId());
        student.setNationality(blankToNull(request.nationality()) == null ? "IN" : request.nationality().trim());
        student.setMotherTongueId(request.motherTongueId());
        student.setAadhaarNo(blankToNull(request.aadhaarNo()));
        student.setApaarId(blankToNull(request.apaarId()));
    }

    private void enroll(Student student, UUID classId, UUID sectionId, UUID yearId, Integer roll) {
        studentService.enroll(student.getId(), new EnrollRequest(classId, sectionId, yearId, roll));
    }

    private void initSteps(Student student) {
        List<StudentStepStatus> existing = stepStatusRepository
                .findByStudentIdAndSchoolIdOrderByStepNoAsc(student.getId(), student.getSchoolId());
        if (!existing.isEmpty()) {
            return;
        }
        for (int n = 1; n <= 6; n++) {
            StudentStepStatus row = new StudentStepStatus();
            row.setSchoolId(student.getSchoolId());
            row.setStudentId(student.getId());
            row.setStepNo(n);
            row.setStatus(n == 1 ? AdmissionStepStatus.COMPLETED : AdmissionStepStatus.PENDING);
            stepStatusRepository.save(row);
        }
    }

    private void markStep(Student student, int stepNo, AdmissionStepStatus status) {
        StudentStepStatus row = stepStatusRepository.findByStudentIdAndStepNo(student.getId(), stepNo)
                .orElseGet(() -> {
                    StudentStepStatus created = new StudentStepStatus();
                    created.setSchoolId(student.getSchoolId());
                    created.setStudentId(student.getId());
                    created.setStepNo(stepNo);
                    return created;
                });
        row.setStatus(status);
        stepStatusRepository.save(row);
        boolean pending = stepStatusRepository
                .findByStudentIdAndSchoolIdOrderByStepNoAsc(student.getId(), student.getSchoolId())
                .stream()
                .anyMatch(s -> s.getStepNo() > 1 && s.getStatus() != AdmissionStepStatus.COMPLETED);
        student.setProfilePending(pending);
        studentRepository.save(student);
    }

    private void upsertFamily(Student student, GuardianRelationship relationship, FamilyMemberDto dto) {
        if (!hasName(dto)) {
            return;
        }
        UUID schoolId = student.getSchoolId();
        StudentGuardian link = studentGuardianRepository.findWithGuardians(schoolId, student.getId()).stream()
                .filter(sg -> sg.getRelationship() == relationship)
                .findFirst()
                .orElse(null);
        Guardian guardian = link == null ? new Guardian() : link.getGuardian();
        guardian.setSchoolId(schoolId);
        guardian.setRelationship(relationship);
        guardian.setFirstName(dto.firstName().trim());
        guardian.setLastName(blankToNull(dto.lastName()));
        guardian.setOccupation(blankToNull(dto.occupation()));
        guardian.setPhone(blankToNull(dto.phone()));
        guardian.setEmail(blankToNull(dto.email()));
        guardian.setQualification(blankToNull(dto.qualification()));
        guardian.setAddress(blankToNull(dto.address()));
        guardian = guardianRepository.save(guardian);
        if (link == null) {
            link = new StudentGuardian();
            link.setSchoolId(schoolId);
            link.setStudent(student);
            link.setRelationship(relationship);
        }
        link.setGuardian(guardian);
        studentGuardianRepository.save(link);
    }

    private void applyPrimary(Student student, String primaryContact) {
        if (primaryContact == null || primaryContact.isBlank()) {
            return;
        }
        GuardianRelationship target = parseRelationship(primaryContact);
        List<StudentGuardian> links = studentGuardianRepository
                .findByStudentIdAndSchoolId(student.getId(), student.getSchoolId());
        for (StudentGuardian link : links) {
            link.setPrimary(link.getRelationship() == target);
            studentGuardianRepository.save(link);
        }
    }

    private void upsertAddress(Student student, AddressType type, AddressDto dto) {
        if (dto == null) {
            return;
        }
        StudentAddress row = addressRepository.findByStudentIdAndAddressType(student.getId(), type)
                .orElseGet(StudentAddress::new);
        row.setSchoolId(student.getSchoolId());
        row.setStudentId(student.getId());
        row.setAddressType(type);
        row.setLine1(blankToNull(dto.line1()));
        row.setLine2(blankToNull(dto.line2()));
        row.setCity(blankToNull(dto.city()));
        row.setDistrict(blankToNull(dto.district()));
        row.setState(blankToNull(dto.state()));
        row.setPin(blankToNull(dto.pin()));
        addressRepository.save(row);
    }

    private void replaceSubjects(Student student, UUID yearId, List<UUID> requested,
                                 SchoolClass schoolClass, UUID streamId) {
        List<ClassSubjectOptionDto> mapped = classSubjects(schoolClass.getId(), streamId);
        Set<UUID> allowed = mapped.stream().map(ClassSubjectOptionDto::subjectId).collect(Collectors.toSet());
        Set<UUID> compulsory = mapped.stream()
                .filter(ClassSubjectOptionDto::compulsory)
                .map(ClassSubjectOptionDto::subjectId)
                .collect(Collectors.toSet());
        Set<UUID> selected = new HashSet<>(compulsory);
        if (requested != null) {
            for (UUID id : requested) {
                if (id != null && allowed.contains(id)) {
                    selected.add(id);
                }
            }
        }
        studentSubjectRepository.deleteByStudentIdAndAcademicYearId(student.getId(), yearId);
        for (UUID subjectId : selected) {
            StudentSubject row = new StudentSubject();
            row.setSchoolId(student.getSchoolId());
            row.setStudentId(student.getId());
            row.setSubjectId(subjectId);
            row.setAcademicYearId(yearId);
            studentSubjectRepository.save(row);
        }
    }

    private void linkSiblings(Student student, List<UUID> siblingIds) {
        UUID schoolId = student.getSchoolId();
        Guardian shared = studentGuardianRepository.findWithGuardians(schoolId, student.getId()).stream()
                .filter(StudentGuardian::isPrimary)
                .map(StudentGuardian::getGuardian)
                .findFirst()
                .orElse(studentGuardianRepository.findWithGuardians(schoolId, student.getId()).stream()
                        .map(StudentGuardian::getGuardian)
                        .findFirst()
                        .orElse(null));
        if (shared == null) {
            return;
        }
        for (UUID siblingId : siblingIds) {
            if (siblingId == null || siblingId.equals(student.getId())) {
                continue;
            }
            Student sibling = studentRepository.findByIdAndSchoolId(siblingId, schoolId).orElse(null);
            if (sibling == null) {
                continue;
            }
            if (!studentGuardianRepository.existsByStudentIdAndGuardianId(siblingId, shared.getId())) {
                StudentGuardian link = new StudentGuardian();
                link.setSchoolId(schoolId);
                link.setStudent(sibling);
                link.setGuardian(shared);
                link.setRelationship(shared.getRelationship());
                studentGuardianRepository.save(link);
            }
        }
    }

    private AdmissionWizardDto toWizard(Student student, String username, String temporaryPassword) {
        UUID schoolId = student.getSchoolId();
        List<StudentGuardian> links = studentGuardianRepository.findWithGuardians(schoolId, student.getId());
        Map<GuardianRelationship, StudentGuardian> byRel = new EnumMap<>(GuardianRelationship.class);
        for (StudentGuardian link : links) {
            byRel.putIfAbsent(link.getRelationship(), link);
        }
        StudentEnrollment enrollment = currentEnrollment(student);
        SchoolClass schoolClass = enrollment == null ? null : classOf(enrollment, schoolId);
        Section section = enrollment == null ? null
                : sectionRepository.findByIdAndSchoolId(enrollment.getSectionId(), schoolId).orElse(null);
        AcademicYear year = enrollment == null ? null
                : academicYearRepository.findByIdAndSchoolId(enrollment.getAcademicYearId(), schoolId).orElse(null);
        Map<AddressType, StudentAddress> addresses = addressRepository
                .findByStudentIdAndSchoolId(student.getId(), schoolId).stream()
                .collect(Collectors.toMap(StudentAddress::getAddressType, a -> a, (a, b) -> a));
        AddressDto current = addresses.containsKey(AddressType.CURRENT)
                ? AddressDto.from(addresses.get(AddressType.CURRENT)) : null;
        AddressDto permanent = addresses.containsKey(AddressType.PERMANENT)
                ? AddressDto.from(addresses.get(AddressType.PERMANENT)) : null;
        PreviousSchoolDto previous = previousSchoolRepository
                .findByStudentIdAndSchoolId(student.getId(), schoolId)
                .map(PreviousSchoolDto::from)
                .orElse(null);
        List<StudentDocumentDto> docs = documentRepository
                .findByStudentIdAndSchoolIdOrderByCreatedAtDesc(student.getId(), schoolId).stream()
                .map(d -> StudentDocumentDto.from(d, canSeePii() ? minioService.presignedUrl(d.getFileKey()) : null))
                .toList();
        UUID yearId = year == null ? null : year.getId();
        List<UUID> subjectIds = yearId == null ? List.of()
                : studentSubjectRepository.findByStudentIdAndAcademicYearId(student.getId(), yearId).stream()
                .map(StudentSubject::getSubjectId).toList();
        List<SiblingDto> siblings = studentGuardianRepository
                .findSiblingStudentIds(schoolId, student.getId()).stream()
                .filter(id -> !id.equals(student.getId()))
                .map(id -> studentRepository.findByIdAndSchoolId(id, schoolId).orElse(null))
                .filter(Objects::nonNull)
                .map(this::toSibling)
                .toList();
        List<StepStatusDto> steps = ensureSteps(student).stream().map(StepStatusDto::from).toList();
        String aadhaar = maskAadhaar(student.getAadhaarNo());
        String photo = student.getPhotoUrl() == null ? null : minioService.presignedUrl(student.getPhotoUrl());
        String primary = links.stream().filter(StudentGuardian::isPrimary)
                .map(sg -> sg.getRelationship().name()).findFirst().orElse(null);
        return new AdmissionWizardDto(
                student.getId(), student.getAdmissionNo(), student.getFirstName(), student.getMiddleName(),
                student.getLastName(), student.getDisplayName(), student.getDateOfBirth(), student.getGender(),
                student.getBloodGroup(), student.getReligionId(), student.getCategoryId(), student.getNationality(),
                student.getMotherTongueId(), aadhaar, student.getApaarId(), photo, student.getAdmissionDate(),
                student.getStatus(),
                schoolClass == null ? null : schoolClass.getId(),
                schoolClass == null ? null : schoolClass.getName(),
                schoolClass != null && schoolClass.isStreamEnabled(),
                section == null ? null : section.getId(),
                section == null ? null : section.getName(),
                enrollment == null ? null : enrollment.getRollNumber(),
                year == null ? null : year.getId(),
                year == null ? null : year.getName(),
                student.getHouseId(), student.getMediumId(), student.getStreamId(),
                subjectIds, null, null,
                toFamily(byRel.get(GuardianRelationship.FATHER)),
                toFamily(byRel.get(GuardianRelationship.MOTHER)),
                toFamily(byRel.get(GuardianRelationship.GUARDIAN)),
                byRel.containsKey(GuardianRelationship.GUARDIAN),
                student.getEmergencyName(), student.getEmergencyRelation(), student.getEmergencyContact(),
                primary, current, permanent, sameAddress(current, permanent),
                student.getPhone(), student.getEmail(), previous,
                student.isTransportRequired(), student.isHostelRequired(),
                student.getMedicalNotes(), student.getRemarks(),
                docs, siblings, steps, student.isProfilePending(),
                username, temporaryPassword);
    }

    private List<StudentStepStatus> ensureSteps(Student student) {
        List<StudentStepStatus> rows = stepStatusRepository
                .findByStudentIdAndSchoolIdOrderByStepNoAsc(student.getId(), student.getSchoolId());
        if (rows.size() == 6) {
            return rows;
        }
        initSteps(student);
        return stepStatusRepository.findByStudentIdAndSchoolIdOrderByStepNoAsc(student.getId(), student.getSchoolId());
    }

    private FamilyMemberDto toFamily(StudentGuardian link) {
        if (link == null || link.getGuardian() == null) {
            return null;
        }
        Guardian g = link.getGuardian();
        return new FamilyMemberDto(g.getId(), g.getFirstName(), g.getLastName(), g.getOccupation(),
                g.getPhone(), g.getEmail(), g.getQualification(), link.getRelationship().name(),
                g.getAddress(), link.isPrimary());
    }

    private SiblingDto toSibling(Student student) {
        StudentEnrollment enrollment = currentEnrollment(student);
        String className = null;
        String sectionName = null;
        if (enrollment != null) {
            Section section = sectionRepository.findByIdAndSchoolId(enrollment.getSectionId(), student.getSchoolId())
                    .orElse(null);
            if (section != null) {
                sectionName = section.getName();
                className = classRepository.findByIdAndSchoolId(section.getClassId(), student.getSchoolId())
                        .map(SchoolClass::getName).orElse(null);
            }
        }
        return new SiblingDto(student.getId(), student.getAdmissionNo(), student.getDisplayName(),
                className, sectionName);
    }

    private StudentEnrollment currentEnrollment(Student student) {
        UUID yearId = academicYearRepository.findBySchoolIdAndCurrentTrue(student.getSchoolId())
                .map(AcademicYear::getId).orElse(null);
        if (yearId == null) {
            return enrollmentRepository.findByStudentId(student.getId()).stream().findFirst().orElse(null);
        }
        return enrollmentRepository.findByStudentIdAndAcademicYearId(student.getId(), yearId).orElse(null);
    }

    private SchoolClass classOf(StudentEnrollment enrollment, UUID schoolId) {
        return sectionRepository.findByIdAndSchoolId(enrollment.getSectionId(), schoolId)
                .flatMap(section -> classRepository.findByIdAndSchoolId(section.getClassId(), schoolId))
                .orElse(null);
    }

    private UUID resolveYearId(UUID schoolId, UUID requested) {
        if (requested != null) {
            academicYearRepository.findByIdAndSchoolId(requested, schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("academic_year", requested));
            return requested;
        }
        return academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId)
                .map(AcademicYear::getId)
                .orElseThrow(() -> new BusinessException("academic_year.not_found"));
    }

    private void validateIds(AdmissionStep1Request request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        SchoolClass schoolClass = classRepository.findByIdAndSchoolId(request.classId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("school_class", request.classId()));
        Section section = sectionRepository.findByIdAndSchoolId(request.sectionId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("section", request.sectionId()));
        if (!section.getClassId().equals(schoolClass.getId())) {
            throw new BusinessException("enrollment.not_found");
        }
    }

    private void assertUniqueIds(UUID schoolId, UUID studentId, String aadhaar, String apaar) {
        String aadhaarNo = blankToNull(aadhaar);
        String apaarId = blankToNull(apaar);
        if (aadhaarNo != null) {
            boolean exists = studentId == null
                    ? studentRepository.existsBySchoolIdAndAadhaarNo(schoolId, aadhaarNo)
                    : studentRepository.existsBySchoolIdAndAadhaarNoAndIdNot(schoolId, aadhaarNo, studentId);
            if (exists) {
                throw new BusinessException("student.aadhaar_exists");
            }
        }
        if (apaarId != null) {
            boolean exists = studentId == null
                    ? studentRepository.existsBySchoolIdAndApaarId(schoolId, apaarId)
                    : studentRepository.existsBySchoolIdAndApaarIdAndIdNot(schoolId, apaarId, studentId);
            if (exists) {
                throw new BusinessException("student.apaar_exists");
            }
        }
    }

    private void validateUpload(MultipartFile file, Set<String> types, long max, String code) {
        if (file == null || file.isEmpty() || file.getSize() > max) {
            throw new BusinessException(code);
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!types.contains(contentType)) {
            throw new BusinessException(code);
        }
    }

    private Student findStudent(UUID id) {
        return studentRepository.findByIdAndSchoolId(id, SecurityUtils.currentSchoolId())
                .orElseThrow(() -> ResourceNotFoundException.of("student", id));
    }

    private static void applyStream(Stream stream, StreamRequest request, String name) {
        stream.setName(name);
        stream.setCode(blankToNull(request.code()));
        stream.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        stream.setStatus(request.status() == null || request.status().isBlank() ? "ACTIVE" : request.status());
    }

    private static void applyLookup(SchoolLookup lookup, LookupRequest request, String name) {
        lookup.setName(name);
        lookup.setCode(blankToNull(request.code()));
        lookup.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        lookup.setStatus(request.status() == null || request.status().isBlank() ? "ACTIVE" : request.status());
    }

    private static boolean hasName(FamilyMemberDto dto) {
        return dto != null && dto.firstName() != null && !dto.firstName().isBlank();
    }

    private static GuardianRelationship parseRelationship(String value) {
        try {
            return GuardianRelationship.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return GuardianRelationship.GUARDIAN;
        }
    }

    private static String formatAddress(AddressDto dto) {
        if (dto == null) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (String part : List.of(dto.line1(), dto.line2(), dto.city(), dto.district(), dto.state(), dto.pin())) {
            if (part != null && !part.isBlank()) {
                parts.add(part.trim());
            }
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private static boolean sameAddress(AddressDto a, AddressDto b) {
        if (a == null || b == null) {
            return false;
        }
        return Objects.equals(blankToNull(a.line1()), blankToNull(b.line1()))
                && Objects.equals(blankToNull(a.line2()), blankToNull(b.line2()))
                && Objects.equals(blankToNull(a.city()), blankToNull(b.city()))
                && Objects.equals(blankToNull(a.district()), blankToNull(b.district()))
                && Objects.equals(blankToNull(a.state()), blankToNull(b.state()))
                && Objects.equals(blankToNull(a.pin()), blankToNull(b.pin()));
    }

    private String maskAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.isBlank()) {
            return aadhaar;
        }
        if (canSeePii()) {
            return aadhaar;
        }
        if (aadhaar.length() < 4) {
            return "XXXXXXXXXXXX";
        }
        return "XXXXXXXX" + aadhaar.substring(aadhaar.length() - 4);
    }

    private boolean canSeePii() {
        var principal = SecurityUtils.currentPrincipal();
        return principal.hasRole("ADMIN") || principal.hasRole("SUPER_ADMIN");
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
