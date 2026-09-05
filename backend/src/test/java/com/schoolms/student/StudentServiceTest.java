package com.schoolms.student;

import com.schoolms.TestSecurity;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.academics.Section;
import com.schoolms.academics.SectionRepository;
import com.schoolms.academics.StudentEnrollment;
import com.schoolms.academics.StudentEnrollmentRepository;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.student.dto.EnrollRequest;
import com.schoolms.student.dto.StudentDto;
import com.schoolms.student.dto.StudentRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private GuardianRepository guardianRepository;
    @Mock
    private StudentGuardianRepository studentGuardianRepository;
    @Mock
    private StudentEnrollmentRepository enrollmentRepository;
    @Mock
    private SchoolClassRepository classRepository;
    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private AcademicYearRepository academicYearRepository;

    private StudentService studentService;

    private final UUID schoolId = TestSecurity.SCHOOL_ID;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(studentRepository, guardianRepository,
                studentGuardianRepository, enrollmentRepository, classRepository,
                sectionRepository, academicYearRepository);
        TestSecurity.loginAsAdmin();
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void createStudentPersistsWithSchoolContext() {
        StudentRequest request = new StudentRequest("ADM100", "Meera", "Sharma",
                LocalDate.of(2014, 5, 10), "FEMALE", "B+", "Hindu", "IN",
                LocalDate.of(2025, 4, 1), null);
        when(studentRepository.existsBySchoolIdAndAdmissionNo(schoolId, "ADM100")).thenReturn(false);

        Student saved = new Student();
        saved.setId(UUID.randomUUID());
        saved.setSchoolId(schoolId);
        saved.setAdmissionNo("ADM100");
        saved.setFirstName("Meera");
        saved.setLastName("Sharma");
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student s = invocation.getArgument(0);
            if (s.getId() == null) {
                s.setId(UUID.randomUUID());
            }
            return s;
        });

        StudentDto dto = studentService.create(request);

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        assertEquals(schoolId, captor.getValue().getSchoolId());
        assertEquals("ADM100", captor.getValue().getAdmissionNo());
        assertEquals("Meera", dto.firstName());
    }

    @Test
    void createStudentRejectsDuplicateAdmissionNo() {
        StudentRequest request = new StudentRequest("ADM100", "Meera", "Sharma",
                LocalDate.of(2014, 5, 10), "FEMALE", null, null, null,
                LocalDate.of(2025, 4, 1), null);
        when(studentRepository.existsBySchoolIdAndAdmissionNo(schoolId, "ADM100")).thenReturn(true);

        assertThrows(BusinessException.class, () -> studentService.create(request));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void deactivateSetsInactiveStatus() {
        UUID studentId = UUID.randomUUID();
        Student student = new Student();
        student.setId(studentId);
        student.setSchoolId(schoolId);
        when(studentRepository.findByIdAndSchoolId(studentId, schoolId)).thenReturn(Optional.of(student));

        studentService.deactivate(studentId);

        verify(studentRepository).save(student);
        assertEquals(com.schoolms.common.enums.StudentStatus.INACTIVE, student.getStatus());
    }

    @Test
    void enrollUpdatesExistingEnrollmentSection() {
        UUID studentId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        Student student = new Student();
        student.setId(studentId);
        student.setSchoolId(schoolId);
        when(studentRepository.findByIdAndSchoolId(studentId, schoolId)).thenReturn(Optional.of(student));

        Section section = new Section();
        section.setId(sectionId);
        section.setClassId(classId);
        when(sectionRepository.findByIdAndSchoolId(sectionId, schoolId)).thenReturn(Optional.of(section));
        when(academicYearRepository.findByIdAndSchoolId(yearId, schoolId))
                .thenReturn(Optional.of(new com.schoolms.academics.AcademicYear()));

        StudentEnrollment existing = new StudentEnrollment();
        existing.setStudentId(studentId);
        existing.setAcademicYearId(yearId);
        existing.setSectionId(UUID.randomUUID());
        existing.setRollNumber(4);
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(studentId, yearId))
                .thenReturn(Optional.of(existing));
        when(enrollmentRepository.save(any(StudentEnrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EnrollRequest request = new EnrollRequest(classId, sectionId, yearId, null);
        StudentEnrollment saved = studentService.enroll(studentId, request);

        assertEquals(sectionId, saved.getSectionId());
        assertEquals(4, saved.getRollNumber());
        verify(enrollmentRepository).save(existing);
    }

    @Test
    void enrollChecksSectionBelongsToRequestedClass() {
        UUID studentId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();

        Student student = new Student();
        student.setId(studentId);
        student.setSchoolId(schoolId);
        when(studentRepository.findByIdAndSchoolId(studentId, schoolId)).thenReturn(Optional.of(student));

        Section section = new Section();
        section.setId(sectionId);
        section.setClassId(UUID.randomUUID());
        section.setName("A");
        when(sectionRepository.findByIdAndSchoolId(sectionId, schoolId)).thenReturn(Optional.of(section));

        EnrollRequest request = new EnrollRequest(classId, sectionId, yearId, 1);

        assertThrows(BusinessException.class, () -> studentService.enroll(studentId, request));
        verify(enrollmentRepository, never()).save(any());
    }
}
