package com.schoolms.certificate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.schoolms.certificate.dto.CertificateIssuedDto;
import com.schoolms.certificate.dto.CertificateTemplateDto;
import com.schoolms.certificate.dto.CertificateTemplateRequest;
import com.schoolms.certificate.dto.GenerateCertificateRequest;
import com.schoolms.common.api.PagedResponse;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.school.School;
import com.schoolms.school.SchoolRepository;
import com.schoolms.security.SecurityUtils;
import com.schoolms.security.UserPrincipal;
import com.schoolms.student.Guardian;
import com.schoolms.student.GuardianRepository;
import com.schoolms.student.Student;
import com.schoolms.student.StudentGuardian;
import com.schoolms.student.StudentGuardianRepository;
import com.schoolms.student.StudentRepository;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final CertificateTemplateRepository templateRepository;
    private final CertificateIssuedRepository issuedRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SectionRepository sectionRepository;
    private final SchoolClassRepository classRepository;
    private final TeacherProfileRepository teacherRepository;
    private final TeacherSectionRepository teacherSectionRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final GuardianRepository guardianRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final CertificatePdfService pdfService;
    private final CertificateLifecycleService lifecycleService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<CertificateTemplateDto> listTemplates() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        ensureTemplates(schoolId);
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        return templateRepository.findBySchoolIdOrderByTypeAsc(schoolId).stream()
                .filter(t -> canManage(principal) || t.isActive())
                .map(this::toTemplateDto)
                .toList();
    }

    @Transactional
    public CertificateTemplateDto upsertTemplate(CertificateTemplateRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        if (!canManage(SecurityUtils.currentPrincipal())) {
            throw new AccessDeniedException("auth.access_denied");
        }
        CertificateTemplate template = templateRepository.findBySchoolIdAndType(schoolId, request.type())
                .orElseGet(() -> {
                    CertificateTemplate created = new CertificateTemplate();
                    created.setSchoolId(schoolId);
                    created.setType(request.type());
                    return created;
                });
        template.setHeaderHtml(blankToNull(request.headerHtml()));
        template.setFooterHtml(blankToNull(request.footerHtml()));
        template.setSignatureImageUrl(blankToNull(request.signatureImageUrl()));
        template.setSealImageUrl(blankToNull(request.sealImageUrl()));
        if (request.active() != null) {
            template.setActive(request.active());
        }
        if (request.requiresApproval() != null) {
            template.setRequiresApproval(request.requiresApproval());
        }
        return toTemplateDto(templateRepository.save(template));
    }

    @Transactional
    public CertificateIssuedDto generate(GenerateCertificateRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        if (!canGenerate(principal)) {
            throw new AccessDeniedException("auth.access_denied");
        }
        Student student = studentRepository.findByIdAndSchoolId(request.studentId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("student", request.studentId()));
        assertCanActOnStudent(principal, schoolId, student);
        ensureTemplates(schoolId);
        CertificateTemplate template = templateRepository.findBySchoolIdAndType(schoolId, request.templateType())
                .orElseThrow(() -> new BusinessException("certificate.template_not_found"));
        if (!template.isActive()) {
            throw new BusinessException("certificate.template_inactive");
        }
        if (request.templateType() == CertificateType.TC && !StringUtils.hasText(request.reason())) {
            throw new BusinessException("certificate.reason_required");
        }

        Snapshot snapshot = snapshot(schoolId, student, template, request);
        CertificateIssued issued = new CertificateIssued();
        issued.setSchoolId(schoolId);
        issued.setStudentId(student.getId());
        issued.setTemplateId(template.getId());
        issued.setCertificateType(request.templateType());
        issued.setIssuedByUserId(principal.id());
        issued.setReason(blankToNull(request.reason()));
        issued.setConductRemarks(blankToNull(request.conductRemarks()));
        issued.setAcademicProgress(blankToNull(request.academicProgress()));
        issued.setLastExamAttended(blankToNull(request.lastExamAttended()));
        issued.setDuesLibrary(request.duesLibrary());
        issued.setDuesAccounts(request.duesAccounts());
        issued.setDuesSports(request.duesSports());
        issued.setDataJson(writeJson(snapshot.fields()));
        issued.setDuplicate(Boolean.TRUE.equals(request.duplicate()));

        boolean autoIssue = canApprove(principal) || !template.isRequiresApproval();
        if (autoIssue) {
            issue(issued, snapshot, principal.id());
        } else {
            issued.setStatus(CertificateStatus.DRAFT);
        }
        CertificateIssued saved = issuedRepository.save(issued);
        if (saved.getStatus() == CertificateStatus.ISSUED) {
            lifecycleService.onIssued(saved, student);
            saved = issuedRepository.save(saved);
        }
        return toIssuedDto(saved, snapshot, principal);
    }

    @Transactional
    public CertificateIssuedDto generateFromRequest(CertificateRequest request) {
        UUID schoolId = request.getSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        Student student = studentRepository.findByIdAndSchoolId(request.getStudentId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("student", request.getStudentId()));
        ensureTemplates(schoolId);
        CertificateTemplate template = templateRepository.findBySchoolIdAndType(schoolId, request.getCertificateType())
                .orElseThrow(() -> new BusinessException("certificate.template_not_found"));
        GenerateCertificateRequest generate = new GenerateCertificateRequest(
                student.getId(),
                request.getCertificateType(),
                request.getReason(),
                request.getConductRemarks(),
                request.getAcademicProgress(),
                request.getLastExamAttended(),
                request.getDuesLibrary(),
                request.getDuesAccounts(),
                request.getDuesSports(),
                false);
        Snapshot snapshot = snapshot(schoolId, student, template, generate);
        CertificateIssued issued = new CertificateIssued();
        issued.setSchoolId(schoolId);
        issued.setStudentId(student.getId());
        issued.setTemplateId(template.getId());
        issued.setCertificateType(request.getCertificateType());
        issued.setIssuedByUserId(principal.id());
        issued.setReason(blankToNull(request.getReason()));
        issued.setConductRemarks(blankToNull(request.getConductRemarks()));
        issued.setAcademicProgress(blankToNull(request.getAcademicProgress()));
        issued.setLastExamAttended(blankToNull(request.getLastExamAttended()));
        issued.setDuesLibrary(request.getDuesLibrary());
        issued.setDuesAccounts(request.getDuesAccounts());
        issued.setDuesSports(request.getDuesSports());
        issued.setRequestId(request.getId());
        issued.setDataJson(writeJson(snapshot.fields()));
        issued.setDuplicate(false);
        issue(issued, snapshot, principal.id());
        CertificateIssued saved = issuedRepository.save(issued);
        lifecycleService.onIssued(saved, student);
        saved = issuedRepository.save(saved);
        return toIssuedDto(saved, snapshot, principal);
    }

    @Transactional
    public CertificateIssuedDto approve(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        if (!canApprove(principal)) {
            throw new AccessDeniedException("auth.access_denied");
        }
        CertificateIssued issued = issuedRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("certificate", id));
        if (issued.getStatus() == CertificateStatus.ISSUED) {
            throw new BusinessException("certificate.already_issued");
        }
        Student student = studentRepository.findByIdAndSchoolId(issued.getStudentId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("student", issued.getStudentId()));
        CertificateTemplate template = templateRepository.findByIdAndSchoolId(issued.getTemplateId(), schoolId)
                .orElseThrow(() -> new BusinessException("certificate.template_not_found"));
        Snapshot snapshot = snapshotFromStored(schoolId, student, template, issued);
        issue(issued, snapshot, principal.id());
        issued.setApprovedByUserId(principal.id());
        issued.setApprovedAt(Instant.now());
        CertificateIssued saved = issuedRepository.save(issued);
        lifecycleService.onIssued(saved, student);
        saved = issuedRepository.save(saved);
        return toIssuedDto(saved, snapshot, principal);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CertificateIssuedDto> register(CertificateType type, CertificateStatus status,
                                                        String query, int page, int size) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        if (!canRead(principal)) {
            throw new AccessDeniedException("auth.access_denied");
        }
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Set<UUID> scopedIds = scopedStudentIds(principal, schoolId);
        Page<CertificateIssued> result;
        if (scopedIds != null) {
            if (scopedIds.isEmpty()) {
                return new PagedResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0);
            }
            result = pageScoped(schoolId, scopedIds, type, status, pageable);
        } else {
            result = pageAll(schoolId, type, status, pageable);
        }
        Map<UUID, Student> students = loadStudents(schoolId, result.getContent());
        Map<UUID, User> users = loadUsers(result.getContent());
        List<CertificateIssuedDto> content = result.getContent().stream()
                .filter(row -> matchesQuery(row, students.get(row.getStudentId()), query))
                .map(row -> toIssuedDto(row, students.get(row.getStudentId()), users, principal))
                .toList();
        return new PagedResponse<>(content, result.getNumber(), result.getSize(),
                query == null || query.isBlank() ? result.getTotalElements() : content.size(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<CertificateIssuedDto> listForStudent(UUID studentId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        if (!canRead(principal)) {
            throw new AccessDeniedException("auth.access_denied");
        }
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("student", studentId));
        assertCanViewStudent(principal, schoolId, student);
        Map<UUID, User> users = Map.of();
        List<CertificateIssued> rows = issuedRepository.findBySchoolIdAndStudentIdOrderByCreatedAtDesc(schoolId, studentId);
        Set<UUID> userIds = rows.stream().flatMap(r -> java.util.stream.Stream.of(r.getIssuedByUserId(), r.getApprovedByUserId()))
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (!userIds.isEmpty()) {
            users = userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        }
        Map<UUID, User> userMap = users;
        return rows.stream()
                .filter(row -> canSeeRow(principal, row))
                .map(row -> toIssuedDto(row, student, userMap, principal))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CertificateIssuedDto> mine() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        List<Student> mine = ownStudents(principal, schoolId);
        return mine.stream()
                .flatMap(student -> listForStudent(student.getId()).stream())
                .toList();
    }

    @Transactional
    public byte[] download(UUID id, boolean reprint) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        CertificateIssued issued = issuedRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("certificate", id));
        Student student = studentRepository.findByIdAndSchoolId(issued.getStudentId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("student", issued.getStudentId()));
        assertCanViewStudent(principal, schoolId, student);
        if (!canSeeRow(principal, issued)) {
            throw new AccessDeniedException("auth.access_denied");
        }
        if (issued.getStatus() != CertificateStatus.ISSUED) {
            throw new BusinessException("certificate.not_issued");
        }
        boolean duplicate = reprint || issued.isDuplicate();
        if (reprint && canGenerate(principal) && !issued.isDuplicate()) {
            issued.setDuplicate(true);
            issuedRepository.save(issued);
            duplicate = true;
        }
        CertificateTemplate template = templateRepository.findByIdAndSchoolId(issued.getTemplateId(), schoolId)
                .orElseThrow(() -> new BusinessException("certificate.template_not_found"));
        Snapshot snapshot = snapshotFromStored(schoolId, student, template, issued);
        Map<String, Object> fields = new LinkedHashMap<>(snapshot.fields());
        fields.put("duplicate", duplicate ? "DUPLICATE" : "");
        return pdfService.render(issued.getCertificateType(), fields);
    }

    public HttpHeaders downloadHeaders(String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        return headers;
    }

    private void issue(CertificateIssued issued, Snapshot snapshot, UUID actorId) {
        int year = snapshot.year();
        long seq = issuedRepository.countNumbered(issued.getSchoolId(), issued.getCertificateType(), year) + 1;
        String schoolCode = snapshot.schoolCode();
        issued.setCertificateNo(schoolCode + "-" + issued.getCertificateType().name() + "-" + year + "-" + String.format("%04d", seq));
        issued.setSequenceYear(year);
        issued.setIssuedDate(LocalDate.now());
        issued.setStatus(CertificateStatus.ISSUED);
        issued.setApprovedByUserId(actorId);
        issued.setApprovedAt(Instant.now());
        Map<String, Object> fields = new LinkedHashMap<>(snapshot.fields());
        fields.put("certificateNo", issued.getCertificateNo());
        fields.put("issuedDate", DATE.format(issued.getIssuedDate()));
        fields.put("duplicate", "");
        issued.setDataJson(writeJson(fields));
        byte[] pdf = pdfService.render(issued.getCertificateType(), fields);
        issued.setPdfObjectKey(pdfService.store(issued.getSchoolId(), pdf));
    }

    private void ensureTemplates(UUID schoolId) {
        School school = schoolRepository.findById(schoolId).orElse(null);
        String header = school == null ? "School" : school.getName();
        String footer = "This is a computer generated certificate. Principal signature and school seal to be affixed after printing.";
        for (CertificateType type : CertificateType.values()) {
            if (templateRepository.findBySchoolIdAndType(schoolId, type).isEmpty()) {
                CertificateTemplate template = new CertificateTemplate();
                template.setSchoolId(schoolId);
                template.setType(type);
                template.setHeaderHtml(header);
                template.setFooterHtml(footer);
                template.setActive(true);
                template.setRequiresApproval(true);
                templateRepository.save(template);
            }
        }
    }

    private Snapshot snapshot(UUID schoolId, Student student, CertificateTemplate template,
                              GenerateCertificateRequest request) {
        AcademicYear year = academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId).orElse(null);
        StudentEnrollment enrollment = year == null ? null
                : enrollmentRepository.findByStudentIdAndAcademicYearId(student.getId(), year.getId()).orElse(null);
        Section section = enrollment == null ? null
                : sectionRepository.findByIdAndSchoolId(enrollment.getSectionId(), schoolId).orElse(null);
        SchoolClass klass = section == null ? null
                : classRepository.findByIdAndSchoolId(section.getClassId(), schoolId).orElse(null);
        School school = schoolRepository.findById(schoolId).orElse(null);
        String guardianName = guardianName(student.getId(), schoolId);
        String className = klass == null ? "" : klass.getName();
        String sectionName = section == null ? "" : section.getName();
        String academicYearName = year == null ? "" : year.getName();
        int sequenceYear = year == null || year.getStartDate() == null
                ? LocalDate.now().getYear()
                : year.getStartDate().getYear();
        String schoolCode = school == null || !StringUtils.hasText(school.getCode()) ? "SCH" : school.getCode();
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("schoolName", school == null ? "" : nullToEmpty(school.getName()));
        fields.put("schoolAddress", school == null ? "" : nullToEmpty(school.getAddress()));
        fields.put("schoolPhone", school == null ? "" : nullToEmpty(school.getPhone()));
        fields.put("logoUrl", school == null ? "" : nullToEmpty(school.getLogoUrl()));
        fields.put("headerHtml", firstNonBlank(template.getHeaderHtml(), school == null ? "" : school.getName()));
        fields.put("footerHtml", firstNonBlank(template.getFooterHtml(),
                "This is a computer generated certificate. Principal signature and school seal to be affixed after printing."));
        fields.put("studentName", student.getDisplayName());
        fields.put("admissionNo", student.getAdmissionNo());
        fields.put("dateOfBirth", student.getDateOfBirth() == null ? "" : DATE.format(student.getDateOfBirth()));
        fields.put("className", className);
        fields.put("sectionName", sectionName);
        fields.put("guardianName", guardianName);
        fields.put("academicYear", academicYearName);
        fields.put("reason", nullToEmpty(request.reason()));
        fields.put("conductRemarks", nullToEmpty(request.conductRemarks()));
        fields.put("academicProgress", nullToEmpty(request.academicProgress()));
        fields.put("lastExamAttended", nullToEmpty(request.lastExamAttended()));
        fields.put("certificateNo", "");
        fields.put("issuedDate", "");
        fields.put("duplicate", "");
        fields.put("principalSignLabel", "Principal Signature");
        fields.put("teacherSignLabel", "Class Teacher Signature");
        fields.put("sealLabel", "School Seal");
        return new Snapshot(fields, sequenceYear, schoolCode, className, sectionName, academicYearName, guardianName);
    }

    private Snapshot snapshotFromStored(UUID schoolId, Student student, CertificateTemplate template,
                                        CertificateIssued issued) {
        Map<String, Object> stored = readJson(issued.getDataJson());
        AcademicYear year = academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId).orElse(null);
        StudentEnrollment enrollment = year == null ? null
                : enrollmentRepository.findByStudentIdAndAcademicYearId(student.getId(), year.getId()).orElse(null);
        Section section = enrollment == null ? null
                : sectionRepository.findByIdAndSchoolId(enrollment.getSectionId(), schoolId).orElse(null);
        SchoolClass klass = section == null ? null
                : classRepository.findByIdAndSchoolId(section.getClassId(), schoolId).orElse(null);
        School school = schoolRepository.findById(schoolId).orElse(null);
        String className = firstNonBlank(stringVal(stored.get("className")), klass == null ? "" : klass.getName());
        String sectionName = firstNonBlank(stringVal(stored.get("sectionName")), section == null ? "" : section.getName());
        String academicYearName = firstNonBlank(stringVal(stored.get("academicYear")), year == null ? "" : year.getName());
        String guardianName = firstNonBlank(stringVal(stored.get("guardianName")), guardianName(student.getId(), schoolId));
        int sequenceYear = issued.getSequenceYear() == null
                ? (year == null || year.getStartDate() == null ? LocalDate.now().getYear() : year.getStartDate().getYear())
                : issued.getSequenceYear();
        String schoolCode = school == null || !StringUtils.hasText(school.getCode()) ? "SCH" : school.getCode();
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("schoolName", firstNonBlank(stringVal(stored.get("schoolName")), school == null ? "" : school.getName()));
        fields.put("schoolAddress", firstNonBlank(stringVal(stored.get("schoolAddress")), school == null ? "" : nullToEmpty(school.getAddress())));
        fields.put("schoolPhone", firstNonBlank(stringVal(stored.get("schoolPhone")), school == null ? "" : nullToEmpty(school.getPhone())));
        fields.put("logoUrl", firstNonBlank(stringVal(stored.get("logoUrl")), school == null ? "" : nullToEmpty(school.getLogoUrl())));
        fields.put("headerHtml", firstNonBlank(stringVal(stored.get("headerHtml")),
                firstNonBlank(template.getHeaderHtml(), school == null ? "" : school.getName())));
        fields.put("footerHtml", firstNonBlank(stringVal(stored.get("footerHtml")),
                firstNonBlank(template.getFooterHtml(),
                        "This is a computer generated certificate. Principal signature and school seal to be affixed after printing.")));
        fields.put("studentName", student.getDisplayName());
        fields.put("admissionNo", student.getAdmissionNo());
        fields.put("dateOfBirth", firstNonBlank(stringVal(stored.get("dateOfBirth")),
                student.getDateOfBirth() == null ? "" : DATE.format(student.getDateOfBirth())));
        fields.put("className", className);
        fields.put("sectionName", sectionName);
        fields.put("guardianName", guardianName);
        fields.put("academicYear", academicYearName);
        fields.put("reason", firstNonBlank(issued.getReason(), stringVal(stored.get("reason"))));
        fields.put("conductRemarks", firstNonBlank(issued.getConductRemarks(), stringVal(stored.get("conductRemarks"))));
        fields.put("academicProgress", firstNonBlank(issued.getAcademicProgress(), stringVal(stored.get("academicProgress"))));
        fields.put("lastExamAttended", firstNonBlank(issued.getLastExamAttended(), stringVal(stored.get("lastExamAttended"))));
        fields.put("certificateNo", firstNonBlank(issued.getCertificateNo(), stringVal(stored.get("certificateNo"))));
        fields.put("issuedDate", issued.getIssuedDate() == null ? stringVal(stored.get("issuedDate")) : DATE.format(issued.getIssuedDate()));
        fields.put("duplicate", issued.isDuplicate() ? "DUPLICATE" : "");
        fields.put("principalSignLabel", "Principal Signature");
        fields.put("teacherSignLabel", "Class Teacher Signature");
        fields.put("sealLabel", "School Seal");
        return new Snapshot(fields, sequenceYear, schoolCode, className, sectionName, academicYearName, guardianName);
    }

    private void assertCanActOnStudent(UserPrincipal principal, UUID schoolId, Student student) {
        if (canApprove(principal)) {
            return;
        }
        if (!isClassTeacherOf(principal, schoolId, student)) {
            throw new AccessDeniedException("auth.access_denied");
        }
    }

    private void assertCanViewStudent(UserPrincipal principal, UUID schoolId, Student student) {
        if (canApprove(principal)) {
            return;
        }
        if (isClassTeacherOf(principal, schoolId, student)) {
            return;
        }
        if (ownStudents(principal, schoolId).stream().anyMatch(s -> s.getId().equals(student.getId()))) {
            return;
        }
        throw new AccessDeniedException("auth.access_denied");
    }

    private boolean isClassTeacherOf(UserPrincipal principal, UUID schoolId, Student student) {
        if (!principal.hasRole("TEACHER")) {
            return false;
        }
        TeacherProfile teacher = teacherRepository.findBySchoolIdAndUserId(schoolId, principal.id()).orElse(null);
        if (teacher == null) {
            return false;
        }
        AcademicYear year = academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId).orElse(null);
        if (year == null) {
            return false;
        }
        StudentEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndAcademicYearId(student.getId(), year.getId()).orElse(null);
        if (enrollment == null) {
            return false;
        }
        UUID yearId = year.getId();
        return teacherSectionRepository.findByTeacherIdAndSchoolId(teacher.getId(), schoolId).stream()
                .anyMatch(assignment -> assignment.getSectionId().equals(enrollment.getSectionId())
                        && assignment.isClassTeacher()
                        && yearId.equals(assignment.getAcademicYearId()));
    }

    private Set<UUID> scopedStudentIds(UserPrincipal principal, UUID schoolId) {
        if (canApprove(principal)) {
            return null;
        }
        if (principal.hasRole("TEACHER")) {
            TeacherProfile teacher = teacherRepository.findBySchoolIdAndUserId(schoolId, principal.id()).orElse(null);
            if (teacher == null) {
                return Set.of();
            }
            AcademicYear year = academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId).orElse(null);
            if (year == null) {
                return Set.of();
            }
            UUID yearId = year.getId();
            Set<UUID> sectionIds = teacherSectionRepository.findByTeacherIdAndSchoolId(teacher.getId(), schoolId).stream()
                    .filter(a -> a.isClassTeacher() && yearId.equals(a.getAcademicYearId()))
                    .map(a -> a.getSectionId())
                    .collect(Collectors.toSet());
            if (sectionIds.isEmpty()) {
                return Set.of();
            }
            return sectionIds.stream()
                    .flatMap(sectionId -> studentRepository.findBySectionAndYear(schoolId, sectionId, yearId).stream())
                    .map(Student::getId)
                    .collect(Collectors.toSet());
        }
        return ownStudents(principal, schoolId).stream().map(Student::getId).collect(Collectors.toSet());
    }

    private List<Student> ownStudents(UserPrincipal principal, UUID schoolId) {
        if (principal.hasRole("PARENT") && !principal.hasRole("ADMIN") && !principal.hasRole("SUPER_ADMIN")
                && !principal.hasRole("TEACHER")) {
            return guardianRepository.findBySchoolIdAndUserId(schoolId, principal.id())
                    .map(guardian -> studentGuardianRepository.findWithStudents(schoolId, guardian.getId()).stream()
                            .map(StudentGuardian::getStudent)
                            .toList())
                    .orElse(List.of());
        }
        if (principal.hasRole("STUDENT")) {
            return studentRepository.findBySchoolIdAndUserId(schoolId, principal.id())
                    .map(List::of)
                    .orElse(List.of());
        }
        return List.of();
    }

    private boolean canSeeRow(UserPrincipal principal, CertificateIssued row) {
        if (canApprove(principal) || principal.hasRole("TEACHER")) {
            return true;
        }
        return row.getStatus() == CertificateStatus.ISSUED;
    }

    private Page<CertificateIssued> pageAll(UUID schoolId, CertificateType type, CertificateStatus status,
                                            PageRequest pageable) {
        if (type != null && status != null) {
            return issuedRepository.findBySchoolIdAndCertificateTypeAndStatusOrderByCreatedAtDesc(
                    schoolId, type, status, pageable);
        }
        if (type != null) {
            return issuedRepository.findBySchoolIdAndCertificateTypeOrderByCreatedAtDesc(schoolId, type, pageable);
        }
        if (status != null) {
            return issuedRepository.findBySchoolIdAndStatusOrderByCreatedAtDesc(schoolId, status, pageable);
        }
        return issuedRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId, pageable);
    }

    private Page<CertificateIssued> pageScoped(UUID schoolId, Set<UUID> studentIds, CertificateType type,
                                               CertificateStatus status, PageRequest pageable) {
        if (type != null && status != null) {
            return issuedRepository.findBySchoolIdAndStudentIdInAndCertificateTypeAndStatusOrderByCreatedAtDesc(
                    schoolId, studentIds, type, status, pageable);
        }
        if (type != null) {
            return issuedRepository.findBySchoolIdAndStudentIdInAndCertificateTypeOrderByCreatedAtDesc(
                    schoolId, studentIds, type, pageable);
        }
        if (status != null) {
            return issuedRepository.findBySchoolIdAndStudentIdInAndStatusOrderByCreatedAtDesc(
                    schoolId, studentIds, status, pageable);
        }
        return issuedRepository.findBySchoolIdAndStudentIdInOrderByCreatedAtDesc(schoolId, studentIds, pageable);
    }

    private Map<UUID, Student> loadStudents(UUID schoolId, List<CertificateIssued> rows) {
        List<UUID> ids = rows.stream().map(CertificateIssued::getStudentId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return studentRepository.findBySchoolIdAndIdIn(schoolId, ids).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));
    }

    private Map<UUID, User> loadUsers(List<CertificateIssued> rows) {
        Set<UUID> ids = rows.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getIssuedByUserId(), r.getApprovedByUserId()))
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream().collect(Collectors.toMap(User::getId, u -> u));
    }

    private boolean matchesQuery(CertificateIssued row, Student student, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String q = query.toLowerCase();
        String name = student == null ? "" : student.getDisplayName().toLowerCase();
        String admission = student == null ? "" : nullToEmpty(student.getAdmissionNo()).toLowerCase();
        String no = nullToEmpty(row.getCertificateNo()).toLowerCase();
        return name.contains(q) || admission.contains(q) || no.contains(q);
    }

    private CertificateTemplateDto toTemplateDto(CertificateTemplate template) {
        return new CertificateTemplateDto(
                template.getId(),
                template.getType(),
                template.getHeaderHtml(),
                template.getFooterHtml(),
                template.getSignatureImageUrl(),
                template.getSealImageUrl(),
                template.isActive(),
                template.isRequiresApproval());
    }

    private CertificateIssuedDto toIssuedDto(CertificateIssued issued, Snapshot snapshot, UserPrincipal principal) {
        User issuer = userRepository.findById(issued.getIssuedByUserId()).orElse(null);
        User approver = issued.getApprovedByUserId() == null
                ? null
                : userRepository.findById(issued.getApprovedByUserId()).orElse(null);
        Student student = studentRepository.findById(issued.getStudentId()).orElse(null);
        return new CertificateIssuedDto(
                issued.getId(),
                issued.getStudentId(),
                student == null ? "" : student.getDisplayName(),
                student == null ? "" : student.getAdmissionNo(),
                snapshot.className(),
                snapshot.sectionName(),
                issued.getCertificateType(),
                issued.getCertificateNo(),
                issued.getIssuedDate(),
                issued.getIssuedByUserId(),
                issuer == null ? "" : issuer.getDisplayName(),
                issued.getApprovedByUserId(),
                approver == null ? "" : approver.getDisplayName(),
                issued.getApprovedAt(),
                issued.getStatus(),
                issued.getReason(),
                issued.getConductRemarks(),
                snapshot.guardianName(),
                snapshot.academicYearName(),
                student == null || student.getDateOfBirth() == null ? "" : DATE.format(student.getDateOfBirth()),
                issued.isDuplicate(),
                canApprove(principal) && issued.getStatus() == CertificateStatus.DRAFT,
                issued.getStatus() == CertificateStatus.ISSUED);
    }

    private CertificateIssuedDto toIssuedDto(CertificateIssued issued, Student student, Map<UUID, User> users,
                                             UserPrincipal principal) {
        User issuer = users.get(issued.getIssuedByUserId());
        User approver = issued.getApprovedByUserId() == null ? null : users.get(issued.getApprovedByUserId());
        Map<String, Object> stored = readJson(issued.getDataJson());
        return new CertificateIssuedDto(
                issued.getId(),
                issued.getStudentId(),
                student == null ? "" : student.getDisplayName(),
                student == null ? "" : student.getAdmissionNo(),
                stringVal(stored.get("className")),
                stringVal(stored.get("sectionName")),
                issued.getCertificateType(),
                issued.getCertificateNo(),
                issued.getIssuedDate(),
                issued.getIssuedByUserId(),
                issuer == null ? "" : issuer.getDisplayName(),
                issued.getApprovedByUserId(),
                approver == null ? "" : approver.getDisplayName(),
                issued.getApprovedAt(),
                issued.getStatus(),
                issued.getReason(),
                issued.getConductRemarks(),
                stringVal(stored.get("guardianName")),
                stringVal(stored.get("academicYear")),
                student == null || student.getDateOfBirth() == null ? stringVal(stored.get("dateOfBirth"))
                        : DATE.format(student.getDateOfBirth()),
                issued.isDuplicate(),
                canApprove(principal) && issued.getStatus() == CertificateStatus.DRAFT,
                issued.getStatus() == CertificateStatus.ISSUED);
    }

    private String guardianName(UUID studentId, UUID schoolId) {
        List<StudentGuardian> links = studentGuardianRepository.findWithGuardians(schoolId, studentId);
        for (StudentGuardian link : links) {
            if (link.isPrimary() && link.getGuardian() != null) {
                return link.getGuardian().getDisplayName();
            }
        }
        return links.stream().findFirst().map(StudentGuardian::getGuardian).map(Guardian::getDisplayName).orElse("");
    }

    private static boolean canRead(UserPrincipal principal) {
        return principal.permissions().contains("CERTIFICATE_READ")
                || principal.hasRole("ADMIN")
                || principal.hasRole("SUPER_ADMIN");
    }

    private static boolean canGenerate(UserPrincipal principal) {
        return principal.permissions().contains("CERTIFICATE_GENERATE")
                || principal.hasRole("ADMIN")
                || principal.hasRole("SUPER_ADMIN");
    }

    private static boolean canManage(UserPrincipal principal) {
        return principal.permissions().contains("CERTIFICATE_MANAGE")
                || principal.hasRole("ADMIN")
                || principal.hasRole("SUPER_ADMIN");
    }

    private static boolean canApprove(UserPrincipal principal) {
        return principal.permissions().contains("CERTIFICATE_APPROVE")
                || principal.hasRole("ADMIN")
                || principal.hasRole("SUPER_ADMIN");
    }

    private String writeJson(Map<String, Object> fields) {
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (JsonProcessingException e) {
            throw new BusinessException("certificate.export_failed");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String stringVal(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : nullToEmpty(second);
    }

    private record Snapshot(Map<String, Object> fields, int year, String schoolCode,
                            String className, String sectionName, String academicYearName, String guardianName) {
    }
}
