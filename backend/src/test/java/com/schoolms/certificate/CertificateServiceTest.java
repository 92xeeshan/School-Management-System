package com.schoolms.certificate;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.schoolms.certificate.dto.GenerateCertificateRequest;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.school.School;
import com.schoolms.school.SchoolRepository;
import com.schoolms.student.GuardianRepository;
import com.schoolms.student.Student;
import com.schoolms.student.StudentGuardianRepository;
import com.schoolms.student.StudentRepository;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    private static final UUID YEAR_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID SECTION_A = UUID.fromString("30000000-0000-0000-0000-000000000021");
    private static final UUID SECTION_B = UUID.fromString("30000000-0000-0000-0000-000000000022");
    private static final UUID CLASS_ID = UUID.fromString("30000000-0000-0000-0000-000000000011");
    private static final UUID TEACHER_PROFILE = UUID.fromString("30000000-0000-0000-0000-000000000041");
    private static final UUID STUDENT_A = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID STUDENT_B = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final UUID TEMPLATE_ID = UUID.fromString("70000000-0000-0000-0000-000000000001");

    @Mock private CertificateTemplateRepository templateRepository;
    @Mock private CertificateIssuedRepository issuedRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private StudentEnrollmentRepository enrollmentRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private SchoolClassRepository classRepository;
    @Mock private TeacherProfileRepository teacherRepository;
    @Mock private TeacherSectionRepository teacherSectionRepository;
    @Mock private StudentGuardianRepository studentGuardianRepository;
    @Mock private GuardianRepository guardianRepository;
    @Mock private SchoolRepository schoolRepository;
    @Mock private UserRepository userRepository;
    @Mock private CertificatePdfService pdfService;
    @Mock private CertificateLifecycleService lifecycleService;

    private CertificateService service;

    @BeforeEach
    void setUp() {
        service = new CertificateService(
                templateRepository, issuedRepository, studentRepository, enrollmentRepository,
                academicYearRepository, sectionRepository, classRepository, teacherRepository,
                teacherSectionRepository, studentGuardianRepository, guardianRepository,
                schoolRepository, userRepository, pdfService, lifecycleService, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void classTeacherCannotGenerateForOtherSection() {
        loginTeacher();
        when(studentRepository.findByIdAndSchoolId(STUDENT_B, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(student(STUDENT_B, "Ananya")));
        stubClassTeacherScope();
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(STUDENT_B, YEAR_ID))
                .thenReturn(Optional.of(enrollment(STUDENT_B, SECTION_B)));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                service.generate(new GenerateCertificateRequest(STUDENT_B, CertificateType.BONAFIDE, null, "Good")));
        assertEquals("auth.access_denied", ex.getMessage());
        verify(issuedRepository, never()).save(any());
    }

    @Test
    void classTeacherSubmitCreatesDraft() {
        loginTeacher();
        stubGenerateHappyPath(STUDENT_A, SECTION_A, false);
        when(issuedRepository.save(any(CertificateIssued.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(TestSecurity.USER_ID)).thenReturn(Optional.of(user(TestSecurity.USER_ID, "Asha")));
        when(studentRepository.findById(STUDENT_A)).thenReturn(Optional.of(student(STUDENT_A, "Aarav")));

        CertificateIssuedDto dto = service.generate(
                new GenerateCertificateRequest(STUDENT_A, CertificateType.BONAFIDE, null, "Good"));

        assertEquals(CertificateStatus.DRAFT, dto.status());
        assertEquals(null, dto.certificateNo());
        verify(pdfService, never()).render(any(), any());
    }

    @Test
    void adminGenerateIssuesSequentialNumber() {
        loginAdmin();
        stubGenerateHappyPath(STUDENT_A, SECTION_A, true);
        when(issuedRepository.countNumbered(TestSecurity.SCHOOL_ID, CertificateType.TC, 2025)).thenReturn(2L);
        when(pdfService.render(eq(CertificateType.TC), any())).thenReturn(new byte[] {1, 2, 3});
        when(pdfService.store(eq(TestSecurity.SCHOOL_ID), any())).thenReturn("key.pdf");
        when(issuedRepository.save(any(CertificateIssued.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(TestSecurity.USER_ID)).thenReturn(Optional.of(user(TestSecurity.USER_ID, "Admin")));
        when(studentRepository.findById(STUDENT_A)).thenReturn(Optional.of(student(STUDENT_A, "Aarav")));

        CertificateIssuedDto dto = service.generate(
                new GenerateCertificateRequest(STUDENT_A, CertificateType.TC, "Relocation", "Good"));

        assertEquals(CertificateStatus.ISSUED, dto.status());
        assertEquals("DEMO-TC-2025-0003", dto.certificateNo());
        assertFalse(dto.duplicate());
    }

    @Test
    void transferCertificateRequiresReason() {
        loginAdmin();
        when(studentRepository.findByIdAndSchoolId(STUDENT_A, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(student(STUDENT_A, "Aarav")));
        CertificateTemplate template = template(true);
        when(templateRepository.findBySchoolIdAndType(eq(TestSecurity.SCHOOL_ID), any(CertificateType.class)))
                .thenReturn(Optional.of(template));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.generate(new GenerateCertificateRequest(STUDENT_A, CertificateType.TC, "  ", "Good")));
        assertEquals("certificate.reason_required", ex.getCode());
    }

    @Test
    void reprintMarksDuplicateWatermark() {
        loginAdmin();
        CertificateIssued issued = issuedRow();
        when(issuedRepository.findByIdAndSchoolId(issued.getId(), TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(issued));
        when(studentRepository.findByIdAndSchoolId(STUDENT_A, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(student(STUDENT_A, "Aarav")));
        when(templateRepository.findByIdAndSchoolId(TEMPLATE_ID, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(template(true)));
        stubSnapshotLookups(SECTION_A);
        when(pdfService.render(eq(CertificateType.BONAFIDE), any())).thenReturn(new byte[] {9});

        service.download(issued.getId(), true);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(pdfService).render(eq(CertificateType.BONAFIDE), captor.capture());
        assertEquals("DUPLICATE", captor.getValue().get("duplicate"));
        assertTrue(issued.isDuplicate());
    }

    private void loginAdmin() {
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("ADMIN"),
                Set.of("CERTIFICATE_READ", "CERTIFICATE_GENERATE", "CERTIFICATE_MANAGE", "CERTIFICATE_APPROVE"));
    }

    private void loginTeacher() {
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("TEACHER"),
                Set.of("CERTIFICATE_READ", "CERTIFICATE_GENERATE", "STUDENT_READ"));
    }

    private void stubClassTeacherScope() {
        TeacherProfile teacher = new TeacherProfile();
        teacher.setId(TEACHER_PROFILE);
        teacher.setSchoolId(TestSecurity.SCHOOL_ID);
        teacher.setUserId(TestSecurity.USER_ID);
        when(teacherRepository.findBySchoolIdAndUserId(TestSecurity.SCHOOL_ID, TestSecurity.USER_ID))
                .thenReturn(Optional.of(teacher));
        AcademicYear year = year();
        when(academicYearRepository.findBySchoolIdAndCurrentTrue(TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(year));
        TeacherSection link = new TeacherSection();
        link.setTeacherId(TEACHER_PROFILE);
        link.setSectionId(SECTION_A);
        link.setAcademicYearId(YEAR_ID);
        link.setClassTeacher(true);
        when(teacherSectionRepository.findByTeacherIdAndSchoolId(TEACHER_PROFILE, TestSecurity.SCHOOL_ID))
                .thenReturn(List.of(link));
    }

    private void stubGenerateHappyPath(UUID studentId, UUID sectionId, boolean admin) {
        when(studentRepository.findByIdAndSchoolId(studentId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(student(studentId, "Aarav")));
        if (!admin) {
            stubClassTeacherScope();
        }
        CertificateTemplate template = template(true);
        when(templateRepository.findBySchoolIdAndType(eq(TestSecurity.SCHOOL_ID), any(CertificateType.class)))
                .thenReturn(Optional.of(template));
        stubSnapshotLookups(sectionId);
    }

    private void stubSnapshotLookups(UUID sectionId) {
        AcademicYear year = year();
        when(academicYearRepository.findBySchoolIdAndCurrentTrue(TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(year));
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(any(), eq(YEAR_ID)))
                .thenReturn(Optional.of(enrollment(STUDENT_A, sectionId)));
        Section section = new Section();
        section.setId(sectionId);
        section.setClassId(CLASS_ID);
        section.setName("A");
        when(sectionRepository.findByIdAndSchoolId(sectionId, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(section));
        SchoolClass klass = new SchoolClass();
        klass.setId(CLASS_ID);
        klass.setName("Class 5");
        when(classRepository.findByIdAndSchoolId(CLASS_ID, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(klass));
        School school = new School();
        school.setId(TestSecurity.SCHOOL_ID);
        school.setCode("DEMO");
        school.setName("Demo Public School");
        when(schoolRepository.findById(TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(school));
        when(studentGuardianRepository.findWithGuardians(eq(TestSecurity.SCHOOL_ID), any())).thenReturn(List.of());
    }

    private CertificateIssued issuedRow() {
        CertificateIssued issued = new CertificateIssued();
        issued.setId(UUID.fromString("80000000-0000-0000-0000-000000000001"));
        issued.setSchoolId(TestSecurity.SCHOOL_ID);
        issued.setStudentId(STUDENT_A);
        issued.setTemplateId(TEMPLATE_ID);
        issued.setCertificateType(CertificateType.BONAFIDE);
        issued.setCertificateNo("DEMO-BONAFIDE-2025-0001");
        issued.setSequenceYear(2025);
        issued.setIssuedDate(LocalDate.of(2026, 4, 1));
        issued.setIssuedByUserId(TestSecurity.USER_ID);
        issued.setStatus(CertificateStatus.ISSUED);
        issued.setDuplicate(false);
        issued.setDataJson("{\"studentName\":\"Aarav Kumar\",\"className\":\"Class 5\",\"sectionName\":\"A\"}");
        return issued;
    }

    private CertificateTemplate template(boolean requiresApproval) {
        CertificateTemplate template = new CertificateTemplate();
        template.setId(TEMPLATE_ID);
        template.setSchoolId(TestSecurity.SCHOOL_ID);
        template.setType(CertificateType.BONAFIDE);
        template.setActive(true);
        template.setRequiresApproval(requiresApproval);
        template.setHeaderHtml("Demo Public School");
        template.setFooterHtml("Sign after printing");
        return template;
    }

    private Student student(UUID id, String first) {
        Student student = new Student();
        student.setId(id);
        student.setSchoolId(TestSecurity.SCHOOL_ID);
        student.setAdmissionNo("ADM" + id.toString().substring(32));
        student.setFirstName(first);
        student.setLastName("Kumar");
        student.setDateOfBirth(LocalDate.of(2014, 5, 10));
        return student;
    }

    private StudentEnrollment enrollment(UUID studentId, UUID sectionId) {
        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudentId(studentId);
        enrollment.setSectionId(sectionId);
        enrollment.setAcademicYearId(YEAR_ID);
        enrollment.setSchoolId(TestSecurity.SCHOOL_ID);
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

    private User user(UUID id, String first) {
        User user = new User();
        user.setId(id);
        user.setFirstName(first);
        user.setLastName("User");
        return user;
    }
}
