package com.schoolms.student;

import com.schoolms.TestSecurity;
import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.ClassSubjectRepository;
import com.schoolms.academics.SchoolClass;
import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.academics.SchoolLookupRepository;
import com.schoolms.academics.Section;
import com.schoolms.academics.SectionRepository;
import com.schoolms.academics.StreamRepository;
import com.schoolms.academics.StudentEnrollment;
import com.schoolms.academics.StudentEnrollmentRepository;
import com.schoolms.academics.SubjectRepository;
import com.schoolms.auth.CredentialAccountService;
import com.schoolms.common.RecordIdGenerator;
import com.schoolms.common.enums.AdmissionStepStatus;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.file.MinioService;
import com.schoolms.student.dto.AdmissionStep1Request;
import com.schoolms.student.dto.AdmissionWizardDto;
import com.schoolms.user.dto.ProvisionedAccount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionServiceTest {

    @Mock private StudentRepository studentRepository;
    @Mock private GuardianRepository guardianRepository;
    @Mock private StudentGuardianRepository studentGuardianRepository;
    @Mock private StudentEnrollmentRepository enrollmentRepository;
    @Mock private StudentStepStatusRepository stepStatusRepository;
    @Mock private StudentAddressRepository addressRepository;
    @Mock private StudentPreviousSchoolRepository previousSchoolRepository;
    @Mock private StudentDocumentRepository documentRepository;
    @Mock private StudentSubjectRepository studentSubjectRepository;
    @Mock private SchoolClassRepository classRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private StreamRepository streamRepository;
    @Mock private SchoolLookupRepository lookupRepository;
    @Mock private ClassSubjectRepository classSubjectRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private StudentService studentService;
    @Mock private CredentialAccountService credentialAccountService;
    @Mock private RecordIdGenerator recordIdGenerator;
    @Mock private MinioService minioService;

    private AdmissionService admissionService;
    private final UUID schoolId = TestSecurity.SCHOOL_ID;
    private final UUID classId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();
    private final UUID yearId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        admissionService = new AdmissionService(studentRepository, guardianRepository, studentGuardianRepository,
                enrollmentRepository, stepStatusRepository, addressRepository, previousSchoolRepository,
                documentRepository, studentSubjectRepository, classRepository, sectionRepository,
                academicYearRepository, streamRepository, lookupRepository, classSubjectRepository,
                subjectRepository, studentService, credentialAccountService, recordIdGenerator, minioService);
        TestSecurity.loginAsAdmin();
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void createRejectsDuplicateApaar() {
        stubClassSectionYear();
        when(recordIdGenerator.nextAdmissionNo(schoolId)).thenReturn("ADM2026001");
        when(studentRepository.existsBySchoolIdAndAdmissionNo(schoolId, "ADM2026001")).thenReturn(false);
        when(studentRepository.existsBySchoolIdAndApaarId(schoolId, "123456789012")).thenReturn(true);

        assertThrows(BusinessException.class, () -> admissionService.create(step1("123456789012")));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void createPersistsStep1AndMarksCompleted() {
        stubClassSectionYear();
        when(recordIdGenerator.nextAdmissionNo(schoolId)).thenReturn("ADM2026001");
        when(studentRepository.existsBySchoolIdAndAdmissionNo(schoolId, "ADM2026001")).thenReturn(false);
        when(credentialAccountService.provision(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ProvisionedAccount(UUID.randomUUID(), "STD2026001", "Temp@123"));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student s = invocation.getArgument(0);
            if (s.getId() == null) {
                s.setId(UUID.randomUUID());
            }
            return s;
        });
        when(academicYearRepository.findByIdAndSchoolId(yearId, schoolId)).thenReturn(Optional.of(year()));
        when(studentService.enroll(any(), any())).thenReturn(new StudentEnrollment());
        when(stepStatusRepository.findByStudentIdAndSchoolIdOrderByStepNoAsc(any(), any())).thenReturn(pendingSteps());
        when(stepStatusRepository.findByStudentIdAndStepNo(any(), eq(1))).thenReturn(Optional.empty());
        when(stepStatusRepository.save(any(StudentStepStatus.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentGuardianRepository.findWithGuardians(any(), any())).thenReturn(List.of());
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(any(), any())).thenReturn(Optional.empty());
        when(academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId)).thenReturn(Optional.of(year()));
        when(addressRepository.findByStudentIdAndSchoolId(any(), any())).thenReturn(List.of());
        when(previousSchoolRepository.findByStudentIdAndSchoolId(any(), any())).thenReturn(Optional.empty());
        when(documentRepository.findByStudentIdAndSchoolIdOrderByCreatedAtDesc(any(), any())).thenReturn(List.of());
        when(studentGuardianRepository.findSiblingStudentIds(any(), any())).thenReturn(List.of());

        AdmissionWizardDto dto = admissionService.create(step1(null));

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertEquals("Aarav", captor.getValue().getFirstName());
        assertEquals("ADM2026001", dto.admissionNo());
        assertTrue(dto.profilePending());
        verify(studentService).enroll(any(), any());
    }

    @Test
    void createRejectsDuplicateAadhaar() {
        stubClassSectionYear();
        when(recordIdGenerator.nextAdmissionNo(schoolId)).thenReturn("ADM2026001");
        when(studentRepository.existsBySchoolIdAndAdmissionNo(schoolId, "ADM2026001")).thenReturn(false);
        when(studentRepository.existsBySchoolIdAndAadhaarNo(schoolId, "123456789012")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> admissionService.create(new AdmissionStep1Request(
                        "Aarav", null, "Kumar", classId, sectionId, yearId,
                        LocalDate.of(2014, 5, 10), "MALE", null, null, null, "IN", null,
                        "123456789012", null)));
        assertEquals("student.aadhaar_exists", ex.getCode());
    }

    private void stubClassSectionYear() {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(classId);
        schoolClass.setSchoolId(schoolId);
        schoolClass.setName("Class 6");
        Section section = new Section();
        section.setId(sectionId);
        section.setClassId(classId);
        section.setName("A");
        when(classRepository.findByIdAndSchoolId(classId, schoolId)).thenReturn(Optional.of(schoolClass));
        when(sectionRepository.findByIdAndSchoolId(sectionId, schoolId)).thenReturn(Optional.of(section));
    }

    private List<StudentStepStatus> pendingSteps() {
        List<StudentStepStatus> rows = new java.util.ArrayList<>();
        for (int n = 1; n <= 6; n++) {
            StudentStepStatus row = new StudentStepStatus();
            row.setStepNo(n);
            row.setStatus(n == 1 ? AdmissionStepStatus.COMPLETED : AdmissionStepStatus.PENDING);
            rows.add(row);
        }
        return rows;
    }

    private AcademicYear year() {
        AcademicYear year = new AcademicYear();
        year.setId(yearId);
        year.setName("2025-26");
        year.setCurrent(true);
        return year;
    }

    private AdmissionStep1Request step1(String apaar) {
        return new AdmissionStep1Request("Aarav", null, "Kumar", classId, sectionId, yearId,
                LocalDate.of(2014, 5, 10), "MALE", null, null, null, "IN", null, null, apaar);
    }
}
