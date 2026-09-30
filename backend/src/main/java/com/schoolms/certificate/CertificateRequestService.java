package com.schoolms.certificate;

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
import com.schoolms.certificate.dto.CertificateRequestDto;
import com.schoolms.certificate.dto.RejectCertificateRequest;
import com.schoolms.certificate.dto.ReviewCertificateRequest;
import com.schoolms.certificate.dto.SubmitCertificateRequest;
import com.schoolms.common.api.PagedResponse;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.file.MinioService;
import com.schoolms.notification.NotificationTriggerService;
import com.schoolms.security.SecurityUtils;
import com.schoolms.security.UserPrincipal;
import com.schoolms.student.Guardian;
import com.schoolms.student.GuardianRepository;
import com.schoolms.student.Student;
import com.schoolms.student.StudentGuardian;
import com.schoolms.student.StudentGuardianRepository;
import com.schoolms.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertificateRequestService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final Set<CertificateRequestStatus> PENDING = Set.of(
            CertificateRequestStatus.SUBMITTED, CertificateRequestStatus.TEACHER_REVIEWED);

    private final CertificateRequestRepository requestRepository;
    private final CertificateIssuedRepository issuedRepository;
    private final CertificateService certificateService;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SectionRepository sectionRepository;
    private final SchoolClassRepository classRepository;
    private final TeacherProfileRepository teacherRepository;
    private final TeacherSectionRepository teacherSectionRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final GuardianRepository guardianRepository;
    private final MinioService minioService;
    private final NotificationTriggerService notifications;

    @Transactional
    public CertificateRequestDto submit(SubmitCertificateRequest body, MultipartFile file) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        Student student = ownStudent(principal, schoolId);
        if (requestRepository.existsBySchoolIdAndStudentIdAndCertificateTypeAndStatusIn(
                schoolId, student.getId(), body.type(), PENDING)) {
            throw new BusinessException("certificate.request_pending");
        }
        if (body.type() == CertificateType.TC && !StringUtils.hasText(body.reason())) {
            throw new BusinessException("certificate.reason_required");
        }
        CertificateRequest request = new CertificateRequest();
        request.setSchoolId(schoolId);
        request.setStudentId(student.getId());
        request.setCertificateType(body.type());
        request.setStatus(CertificateRequestStatus.SUBMITTED);
        request.setReason(trimToNull(body.reason()));
        request.setRequestedByUserId(principal.id());
        if (file != null && !file.isEmpty()) {
            try {
                request.setSupportingDocKey(minioService.upload(schoolId, "certificate-docs", file));
                request.setSupportingDocName(file.getOriginalFilename());
            } catch (Exception e) {
                request.setSupportingDocName(file.getOriginalFilename());
            }
        }
        CertificateRequest saved = requestRepository.save(request);
        StudentContext ctx = context(schoolId, student);
        notifications.onCertificateRequested(schoolId, ctx.sectionId(), student.getDisplayName(), typeLabel(body.type()));
        return toDto(saved, student, ctx, principal);
    }

    @Transactional(readOnly = true)
    public List<CertificateRequestDto> mine() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        return ownStudents(principal, schoolId).stream()
                .flatMap(student -> requestRepository
                        .findBySchoolIdAndStudentIdOrderByCreatedAtDesc(schoolId, student.getId()).stream()
                        .map(row -> toDto(row, student, context(schoolId, student), principal)))
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<CertificateRequestDto> list(CertificateType type, CertificateRequestStatus status,
                                                     int page, int size) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        if (!canStaff(principal)) {
            throw new AccessDeniedException("auth.access_denied");
        }
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Set<UUID> scoped = scopedStudentIds(principal, schoolId);
        Page<CertificateRequest> result = pageRequests(schoolId, scoped, type, status, pageable);
        List<CertificateRequestDto> content = result.getContent().stream()
                .map(row -> {
                    Student student = studentRepository.findByIdAndSchoolId(row.getStudentId(), schoolId).orElse(null);
                    return toDto(row, student, student == null ? StudentContext.empty() : context(schoolId, student), principal);
                })
                .toList();
        return new PagedResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public CertificateRequestDto review(UUID id, ReviewCertificateRequest body) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        CertificateRequest request = requestRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("certificate_request", id));
        Student student = studentRepository.findByIdAndSchoolId(request.getStudentId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("student", request.getStudentId()));
        if (!isClassTeacherOf(principal, schoolId, student) && !canApprove(principal)) {
            throw new AccessDeniedException("auth.access_denied");
        }
        if (request.getStatus() != CertificateRequestStatus.SUBMITTED) {
            throw new BusinessException("certificate.request_not_pending");
        }
        if (request.getCertificateType() == CertificateType.TC && !StringUtils.hasText(firstNonBlank(body.reason(), request.getReason()))) {
            throw new BusinessException("certificate.reason_required");
        }
        request.setConductRemarks(trimToNull(body.conductRemarks()));
        request.setAcademicProgress(trimToNull(body.academicProgress()));
        request.setLastExamAttended(trimToNull(body.lastExamAttended()));
        if (StringUtils.hasText(body.reason())) {
            request.setReason(body.reason().trim());
        }
        request.setTeacherNotes(trimToNull(body.teacherNotes()));
        request.setDuesLibrary(body.duesLibrary());
        request.setDuesAccounts(body.duesAccounts());
        request.setDuesSports(body.duesSports());
        request.setStatus(CertificateRequestStatus.TEACHER_REVIEWED);
        request.setReviewedByUserId(principal.id());
        request.setReviewedAt(Instant.now());
        CertificateRequest saved = requestRepository.save(request);
        StudentContext ctx = context(schoolId, student);
        notifications.onCertificateForwarded(schoolId, student.getDisplayName(), typeLabel(request.getCertificateType()));
        return toDto(saved, student, ctx, principal);
    }

    @Transactional
    public CertificateRequestDto cancel(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        CertificateRequest request = requestRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("certificate_request", id));
        Student student = studentRepository.findByIdAndSchoolId(request.getStudentId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("student", request.getStudentId()));
        boolean owner = ownStudents(principal, schoolId).stream().anyMatch(s -> s.getId().equals(student.getId()));
        boolean teacher = isClassTeacherOf(principal, schoolId, student);
        if (!owner && !teacher && !canApprove(principal)) {
            throw new AccessDeniedException("auth.access_denied");
        }
        if (!PENDING.contains(request.getStatus())) {
            throw new BusinessException("certificate.request_not_pending");
        }
        request.setStatus(CertificateRequestStatus.CANCELLED);
        return toDto(requestRepository.save(request), student, context(schoolId, student), principal);
    }

    @Transactional
    public CertificateRequestDto approve(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        if (!canApprove(principal)) {
            throw new AccessDeniedException("auth.access_denied");
        }
        CertificateRequest request = requestRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("certificate_request", id));
        if (request.getStatus() != CertificateRequestStatus.TEACHER_REVIEWED
                && request.getStatus() != CertificateRequestStatus.SUBMITTED) {
            throw new BusinessException("certificate.request_not_pending");
        }
        if (request.getCertificateType() == CertificateType.TC && !StringUtils.hasText(request.getReason())) {
            throw new BusinessException("certificate.reason_required");
        }
        Student student = studentRepository.findByIdAndSchoolId(request.getStudentId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("student", request.getStudentId()));
        var issued = certificateService.generateFromRequest(request);
        request.setStatus(CertificateRequestStatus.ISSUED);
        request.setApprovedByUserId(principal.id());
        request.setApprovedAt(Instant.now());
        request.setIssuedId(issued.id());
        CertificateRequest saved = requestRepository.save(request);
        StudentContext ctx = context(schoolId, student);
        notifications.onCertificateDecision(schoolId, student.getId(), ctx.sectionId(), true, typeLabel(request.getCertificateType()));
        return toDto(saved, student, ctx, principal);
    }

    @Transactional
    public CertificateRequestDto reject(UUID id, RejectCertificateRequest body) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        if (!canApprove(principal)) {
            throw new AccessDeniedException("auth.access_denied");
        }
        CertificateRequest request = requestRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("certificate_request", id));
        if (!PENDING.contains(request.getStatus())) {
            throw new BusinessException("certificate.request_not_pending");
        }
        request.setStatus(CertificateRequestStatus.REJECTED);
        request.setRejectionReason(body.rejectionReason().trim());
        request.setApprovedByUserId(principal.id());
        request.setApprovedAt(Instant.now());
        CertificateRequest saved = requestRepository.save(request);
        Student student = studentRepository.findByIdAndSchoolId(request.getStudentId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("student", request.getStudentId()));
        StudentContext ctx = context(schoolId, student);
        notifications.onCertificateDecision(schoolId, student.getId(), ctx.sectionId(), false, typeLabel(request.getCertificateType()));
        return toDto(saved, student, ctx, principal);
    }

    private Page<CertificateRequest> pageRequests(UUID schoolId, Set<UUID> scoped, CertificateType type,
                                                  CertificateRequestStatus status, PageRequest pageable) {
        if (scoped != null) {
            if (scoped.isEmpty()) {
                return Page.empty(pageable);
            }
            if (type != null && status != null) {
                return requestRepository.findBySchoolIdAndStudentIdInAndCertificateTypeAndStatusOrderByCreatedAtDesc(
                        schoolId, scoped, type, status, pageable);
            }
            if (type != null) {
                return requestRepository.findBySchoolIdAndStudentIdInAndCertificateTypeOrderByCreatedAtDesc(
                        schoolId, scoped, type, pageable);
            }
            if (status != null) {
                return requestRepository.findBySchoolIdAndStudentIdInAndStatusOrderByCreatedAtDesc(
                        schoolId, scoped, status, pageable);
            }
            return requestRepository.findBySchoolIdAndStudentIdInOrderByCreatedAtDesc(schoolId, scoped, pageable);
        }
        if (type != null && status != null) {
            return requestRepository.findBySchoolIdAndCertificateTypeAndStatusOrderByCreatedAtDesc(
                    schoolId, type, status, pageable);
        }
        if (type != null) {
            return requestRepository.findBySchoolIdAndCertificateTypeOrderByCreatedAtDesc(schoolId, type, pageable);
        }
        if (status != null) {
            return requestRepository.findBySchoolIdAndStatusOrderByCreatedAtDesc(schoolId, status, pageable);
        }
        return requestRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId, pageable);
    }

    private Student ownStudent(UserPrincipal principal, UUID schoolId) {
        List<Student> mine = ownStudents(principal, schoolId);
        if (mine.size() != 1 && !principal.hasRole("STUDENT")) {
            if (mine.isEmpty()) {
                throw new AccessDeniedException("auth.access_denied");
            }
        }
        if (mine.isEmpty()) {
            throw new AccessDeniedException("auth.access_denied");
        }
        return mine.getFirst();
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

    private boolean isClassTeacherOf(UserPrincipal principal, UUID schoolId, Student student) {
        if (!principal.hasRole("TEACHER") && !canApprove(principal)) {
            return false;
        }
        if (canApprove(principal) && !principal.hasRole("TEACHER")) {
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

    private StudentContext context(UUID schoolId, Student student) {
        AcademicYear year = academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId).orElse(null);
        StudentEnrollment enrollment = year == null ? null
                : enrollmentRepository.findByStudentIdAndAcademicYearId(student.getId(), year.getId()).orElse(null);
        Section section = enrollment == null ? null
                : sectionRepository.findByIdAndSchoolId(enrollment.getSectionId(), schoolId).orElse(null);
        SchoolClass klass = section == null ? null
                : classRepository.findByIdAndSchoolId(section.getClassId(), schoolId).orElse(null);
        String guardianName = guardianName(student.getId(), schoolId);
        return new StudentContext(
                section == null ? null : section.getId(),
                klass == null ? "" : klass.getName(),
                section == null ? "" : section.getName(),
                enrollment == null || enrollment.getRollNumber() == null ? "" : String.valueOf(enrollment.getRollNumber()),
                student.getDateOfBirth() == null ? "" : DATE.format(student.getDateOfBirth()),
                guardianName);
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

    private CertificateRequestDto toDto(CertificateRequest request, Student student, StudentContext ctx,
                                        UserPrincipal principal) {
        String certificateNo = null;
        boolean canDownload = false;
        if (request.getIssuedId() != null) {
            CertificateIssued issued = issuedRepository.findById(request.getIssuedId()).orElse(null);
            if (issued != null) {
                certificateNo = issued.getCertificateNo();
                canDownload = issued.getStatus() == CertificateStatus.ISSUED;
            }
        }
        boolean teacher = student != null && isClassTeacherOf(principal, request.getSchoolId(), student);
        boolean approver = canApprove(principal);
        boolean owner = student != null && ownStudents(principal, request.getSchoolId()).stream()
                .anyMatch(s -> s.getId().equals(student.getId()));
        return new CertificateRequestDto(
                request.getId(),
                request.getStudentId(),
                student == null ? "" : student.getDisplayName(),
                student == null ? "" : student.getAdmissionNo(),
                ctx.rollNo(),
                ctx.className(),
                ctx.sectionName(),
                ctx.dateOfBirth(),
                ctx.guardianName(),
                request.getCertificateType(),
                request.getStatus(),
                request.getReason(),
                request.getConductRemarks(),
                request.getAcademicProgress(),
                request.getLastExamAttended(),
                request.getDuesLibrary(),
                request.getDuesAccounts(),
                request.getDuesSports(),
                request.getTeacherNotes(),
                request.getRejectionReason(),
                request.getSupportingDocName(),
                request.getIssuedId(),
                certificateNo,
                request.getCreatedAt(),
                request.getReviewedAt(),
                request.getApprovedAt(),
                teacher && request.getStatus() == CertificateRequestStatus.SUBMITTED,
                (owner || teacher || approver) && PENDING.contains(request.getStatus()),
                approver && PENDING.contains(request.getStatus()),
                approver && PENDING.contains(request.getStatus()),
                canDownload);
    }

    private static boolean canApprove(UserPrincipal principal) {
        return principal.permissions().contains("CERTIFICATE_APPROVE")
                || principal.hasRole("ADMIN")
                || principal.hasRole("SUPER_ADMIN");
    }

    private static boolean canStaff(UserPrincipal principal) {
        return principal.permissions().contains("CERTIFICATE_READ")
                || principal.permissions().contains("CERTIFICATE_GENERATE")
                || principal.hasRole("ADMIN")
                || principal.hasRole("SUPER_ADMIN")
                || principal.hasRole("TEACHER");
    }

    private static String typeLabel(CertificateType type) {
        return switch (type) {
            case TC -> "Transfer Certificate";
            case CHARACTER -> "Character Certificate";
            case COURSE_COMPLETION -> "Course Completion Certificate";
            default -> "Bonafide Certificate";
        };
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private record StudentContext(UUID sectionId, String className, String sectionName, String rollNo,
                                  String dateOfBirth, String guardianName) {
        static StudentContext empty() {
            return new StudentContext(null, "", "", "", "", "");
        }
    }
}
