package com.schoolms.certificate;

import com.schoolms.TestSecurity;
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
import com.schoolms.academics.TeacherSection;
import com.schoolms.academics.TeacherSectionRepository;
import com.schoolms.certificate.dto.CertificateIssuedDto;
import com.schoolms.certificate.dto.CertificateRequestDto;
import com.schoolms.certificate.dto.RejectCertificateRequest;
import com.schoolms.certificate.dto.ReviewCertificateRequest;
import com.schoolms.certificate.dto.SubmitCertificateRequest;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.file.MinioService;
import com.schoolms.notification.NotificationTriggerService;
import com.schoolms.student.GuardianRepository;
import com.schoolms.student.Student;
import com.schoolms.student.StudentGuardianRepository;
import com.schoolms.student.StudentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateRequestServiceTest {

    private static final UUID YEAR_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID SECTION_A = UUID.fromString("30000000-0000-0000-0000-000000000021");
    private static final UUID CLASS_ID = UUID.fromString("30000000-0000-0000-0000-000000000011");
    private static final UUID TEACHER_PROFILE = UUID.fromString("30000000-0000-0000-0000-000000000041");
    private static final UUID STUDENT_A = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID REQUEST_ID = UUID.fromString("90000000-0000-0000-0000-000000000001");

    @Mock private CertificateRequestRepository requestRepository;
    @Mock private CertificateIssuedRepository issuedRepository;
    @Mock private CertificateService certificateService;
    @Mock private StudentRepository studentRepository;
    @Mock private StudentEnrollmentRepository enrollmentRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private SchoolClassRepository classRepository;
    @Mock private TeacherProfileRepository teacherRepository;
    @Mock private TeacherSectionRepository teacherSectionRepository;
    @Mock private StudentGuardianRepository studentGuardianRepository;
    @Mock private GuardianRepository guardianRepository;
    @Mock private MinioService minioService;
    @Mock private NotificationTriggerService notifications;

    private CertificateRequestService service;

    @BeforeEach
    void setUp() {
        service = new CertificateRequestService(
                requestRepository, issuedRepository, certificateService, studentRepository, enrollmentRepository,
                academicYearRepository, sectionRepository, classRepository, teacherRepository,
                teacherSectionRepository, studentGuardianRepository, guardianRepository, minioService, notifications);
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void studentSubmitCreatesPendingRequest() {
        loginStudent();
        Student student = student();
        when(studentRepository.findBySchoolIdAndUserId(TestSecurity.SCHOOL_ID, TestSecurity.USER_ID))
                .thenReturn(Optional.of(student));
        when(requestRepository.existsBySchoolIdAndStudentIdAndCertificateTypeAndStatusIn(
                eq(TestSecurity.SCHOOL_ID), eq(STUDENT_A), eq(CertificateType.BONAFIDE), any()))
                .thenReturn(false);
        when(requestRepository.save(any(CertificateRequest.class))).thenAnswer(inv -> {
            CertificateRequest saved = inv.getArgument(0);
            saved.setId(REQUEST_ID);
            return saved;
        });
        stubContext();

        CertificateRequestDto dto = service.submit(new SubmitCertificateRequest(CertificateType.BONAFIDE, "Need proof"), null);

        assertEquals(CertificateRequestStatus.SUBMITTED, dto.status());
        assertEquals(STUDENT_A, dto.studentId());
        verify(notifications).onCertificateRequested(eq(TestSecurity.SCHOOL_ID), eq(SECTION_A), any(), any());
    }

    @Test
    void duplicatePendingRequestIsRejected() {
        loginStudent();
        when(studentRepository.findBySchoolIdAndUserId(TestSecurity.SCHOOL_ID, TestSecurity.USER_ID))
                .thenReturn(Optional.of(student()));
        when(requestRepository.existsBySchoolIdAndStudentIdAndCertificateTypeAndStatusIn(
                eq(TestSecurity.SCHOOL_ID), eq(STUDENT_A), eq(CertificateType.BONAFIDE), any()))
                .thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.submit(new SubmitCertificateRequest(CertificateType.BONAFIDE, "Need proof"), null));
        assertEquals("certificate.request_pending", ex.getCode());
        verify(requestRepository, never()).save(any());
    }

    @Test
    void classTeacherForwardSetsTeacherReviewed() {
        loginTeacher();
        CertificateRequest request = pendingRequest(CertificateRequestStatus.SUBMITTED);
        when(requestRepository.findByIdAndSchoolId(REQUEST_ID, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(request));
        when(studentRepository.findByIdAndSchoolId(STUDENT_A, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(student()));
        stubClassTeacherScope();
        stubContext();
        when(requestRepository.save(any(CertificateRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CertificateRequestDto dto = service.review(REQUEST_ID, new ReviewCertificateRequest(
                "Good", "Satisfactory", "Annual 2025", "Relocation", "Cleared", true, true, true));

        assertEquals(CertificateRequestStatus.TEACHER_REVIEWED, dto.status());
        assertEquals("Good", dto.conductRemarks());
        verify(notifications).onCertificateForwarded(eq(TestSecurity.SCHOOL_ID), any(), any());
    }

    @Test
    void teacherCannotReviewOtherSection() {
        loginTeacher();
        CertificateRequest request = pendingRequest(CertificateRequestStatus.SUBMITTED);
        when(requestRepository.findByIdAndSchoolId(REQUEST_ID, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(request));
        when(studentRepository.findByIdAndSchoolId(STUDENT_A, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(student()));
        TeacherProfile teacher = new TeacherProfile();
        teacher.setId(TEACHER_PROFILE);
        teacher.setSchoolId(TestSecurity.SCHOOL_ID);
        teacher.setUserId(TestSecurity.USER_ID);
        when(teacherRepository.findBySchoolIdAndUserId(TestSecurity.SCHOOL_ID, TestSecurity.USER_ID))
                .thenReturn(Optional.of(teacher));
        when(academicYearRepository.findBySchoolIdAndCurrentTrue(TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(year()));
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(STUDENT_A, YEAR_ID))
                .thenReturn(Optional.of(enrollment()));
        when(teacherSectionRepository.findByTeacherIdAndSchoolId(TEACHER_PROFILE, TestSecurity.SCHOOL_ID))
                .thenReturn(List.of());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                service.review(REQUEST_ID, new ReviewCertificateRequest("Good", null, null, null, null, true, true, true)));
        assertEquals("auth.access_denied", ex.getMessage());
    }

    @Test
    void adminApproveIssuesCertificate() {
        loginAdmin();
        CertificateRequest request = pendingRequest(CertificateRequestStatus.TEACHER_REVIEWED);
        when(requestRepository.findByIdAndSchoolId(REQUEST_ID, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(request));
        when(studentRepository.findByIdAndSchoolId(STUDENT_A, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(student()));
        stubContext();
        when(certificateService.generateFromRequest(request)).thenReturn(
                new CertificateIssuedDto(
                        UUID.fromString("80000000-0000-0000-0000-000000000001"),
                        STUDENT_A, "Aarav Kumar", "ADM0001", "Class 5", "A",
                        CertificateType.BONAFIDE, "DEMO-BONAFIDE-2025-0001",
                        LocalDate.of(2026, 4, 1), TestSecurity.USER_ID, "Admin",
                        TestSecurity.USER_ID, "Admin", null, CertificateStatus.ISSUED,
                        "Need proof", "Good", "Father", "2025-26", "10 May 2014", false, false, true));
        when(requestRepository.save(any(CertificateRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CertificateRequestDto dto = service.approve(REQUEST_ID);

        assertEquals(CertificateRequestStatus.ISSUED, dto.status());
        verify(certificateService).generateFromRequest(request);
        verify(notifications).onCertificateDecision(eq(TestSecurity.SCHOOL_ID), eq(STUDENT_A), eq(SECTION_A), eq(true), any());
    }

    @Test
    void adminRejectRequiresPendingRequest() {
        loginAdmin();
        CertificateRequest request = pendingRequest(CertificateRequestStatus.TEACHER_REVIEWED);
        when(requestRepository.findByIdAndSchoolId(REQUEST_ID, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(CertificateRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentRepository.findByIdAndSchoolId(STUDENT_A, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(student()));
        stubContext();

        CertificateRequestDto dto = service.reject(REQUEST_ID, new RejectCertificateRequest("Incomplete documents"));

        assertEquals(CertificateRequestStatus.REJECTED, dto.status());
        assertEquals("Incomplete documents", dto.rejectionReason());
        verify(notifications).onCertificateDecision(eq(TestSecurity.SCHOOL_ID), eq(STUDENT_A), eq(SECTION_A), eq(false), any());
    }

    @Test
    void transferRequestRequiresReason() {
        loginStudent();
        when(studentRepository.findBySchoolIdAndUserId(TestSecurity.SCHOOL_ID, TestSecurity.USER_ID))
                .thenReturn(Optional.of(student()));
        when(requestRepository.existsBySchoolIdAndStudentIdAndCertificateTypeAndStatusIn(
                eq(TestSecurity.SCHOOL_ID), eq(STUDENT_A), eq(CertificateType.TC), any()))
                .thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.submit(new SubmitCertificateRequest(CertificateType.TC, "  "), null));
        assertEquals("certificate.reason_required", ex.getCode());
    }

    private void loginAdmin() {
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("ADMIN"),
                Set.of("CERTIFICATE_READ", "CERTIFICATE_GENERATE", "CERTIFICATE_MANAGE", "CERTIFICATE_APPROVE"));
    }

    private void loginTeacher() {
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("TEACHER"),
                Set.of("CERTIFICATE_READ", "CERTIFICATE_GENERATE", "STUDENT_READ"));
    }

    private void loginStudent() {
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("STUDENT"),
                Set.of("CERTIFICATE_READ"));
    }

    private void stubClassTeacherScope() {
        TeacherProfile teacher = new TeacherProfile();
        teacher.setId(TEACHER_PROFILE);
        teacher.setSchoolId(TestSecurity.SCHOOL_ID);
        teacher.setUserId(TestSecurity.USER_ID);
        when(teacherRepository.findBySchoolIdAndUserId(TestSecurity.SCHOOL_ID, TestSecurity.USER_ID))
                .thenReturn(Optional.of(teacher));
        when(academicYearRepository.findBySchoolIdAndCurrentTrue(TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(year()));
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(STUDENT_A, YEAR_ID))
                .thenReturn(Optional.of(enrollment()));
        TeacherSection link = new TeacherSection();
        link.setTeacherId(TEACHER_PROFILE);
        link.setSectionId(SECTION_A);
        link.setAcademicYearId(YEAR_ID);
        link.setClassTeacher(true);
        when(teacherSectionRepository.findByTeacherIdAndSchoolId(TEACHER_PROFILE, TestSecurity.SCHOOL_ID))
                .thenReturn(List.of(link));
    }

    private void stubContext() {
        when(academicYearRepository.findBySchoolIdAndCurrentTrue(TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(year()));
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(STUDENT_A, YEAR_ID))
                .thenReturn(Optional.of(enrollment()));
        Section section = new Section();
        section.setId(SECTION_A);
        section.setClassId(CLASS_ID);
        section.setName("A");
        when(sectionRepository.findByIdAndSchoolId(SECTION_A, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(section));
        SchoolClass klass = new SchoolClass();
        klass.setId(CLASS_ID);
        klass.setName("Class 5");
        when(classRepository.findByIdAndSchoolId(CLASS_ID, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(klass));
        when(studentGuardianRepository.findWithGuardians(TestSecurity.SCHOOL_ID, STUDENT_A)).thenReturn(List.of());
    }

    private CertificateRequest pendingRequest(CertificateRequestStatus status) {
        CertificateRequest request = new CertificateRequest();
        request.setId(REQUEST_ID);
        request.setSchoolId(TestSecurity.SCHOOL_ID);
        request.setStudentId(STUDENT_A);
        request.setCertificateType(CertificateType.BONAFIDE);
        request.setStatus(status);
        request.setReason("Need proof");
        request.setRequestedByUserId(TestSecurity.USER_ID);
        return request;
    }

    private Student student() {
        Student student = new Student();
        student.setId(STUDENT_A);
        student.setSchoolId(TestSecurity.SCHOOL_ID);
        student.setUserId(TestSecurity.USER_ID);
        student.setAdmissionNo("ADM0001");
        student.setFirstName("Aarav");
        student.setLastName("Kumar");
        student.setDateOfBirth(LocalDate.of(2014, 5, 10));
        return student;
    }

    private StudentEnrollment enrollment() {
        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudentId(STUDENT_A);
        enrollment.setSectionId(SECTION_A);
        enrollment.setAcademicYearId(YEAR_ID);
        enrollment.setSchoolId(TestSecurity.SCHOOL_ID);
        enrollment.setRollNumber(1);
        return enrollment;
    }

    private AcademicYear year() {
        AcademicYear year = new AcademicYear();
        year.setId(YEAR_ID);
        year.setSchoolId(TestSecurity.SCHOOL_ID);
        year.setName("2025-26");
        year.setStartDate(LocalDate.of(2025, 4, 1));
        year.setEndDate(LocalDate.of(2026, 3, 31));
        year.setCurrent(true);
        return year;
    }
}
