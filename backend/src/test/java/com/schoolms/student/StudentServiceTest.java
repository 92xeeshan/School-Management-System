package com.schoolms.student;

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
import com.schoolms.attendance.AttendanceService;
import com.schoolms.attendance.dto.AttendanceSummaryDto;
import com.schoolms.common.enums.StudentStatus;
import com.schoolms.common.exception.AuthException;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.student.dto.EnrollRequest;
import com.schoolms.student.dto.StudentDto;
import com.schoolms.student.dto.StudentProfileDto;
import com.schoolms.student.dto.StudentProfileUpdateRequest;
import com.schoolms.student.dto.StudentRequest;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    @Mock
    private TeacherProfileRepository teacherRepository;
    @Mock
    private TeacherSectionRepository teacherSectionRepository;
    @Mock
    private AttendanceService attendanceService;

    private StudentService studentService;

    private final UUID schoolId = TestSecurity.SCHOOL_ID;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(studentRepository, guardianRepository,
                studentGuardianRepository, enrollmentRepository, classRepository,
                sectionRepository, academicYearRepository, teacherRepository,
                teacherSectionRepository, attendanceService);
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
                LocalDate.of(2025, 4, 1), null, null, null, null, null, null);
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
                LocalDate.of(2025, 4, 1), null, null, null, null, null, null);
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

    @Test
    void getProfileReturnsAdminEditableFields() {
        UUID studentId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        Student student = sampleStudent(studentId);
        when(studentRepository.findByIdAndSchoolId(studentId, schoolId)).thenReturn(Optional.of(student));
        stubProfileLookups(studentId, yearId, classId, sectionId, student);

        StudentProfileDto profile = studentService.getProfile(studentId);

        assertEquals("ADM100", profile.admissionNo());
        assertTrue(profile.canEdit());
        assertTrue(profile.editableFields().contains("firstName"));
        assertTrue(profile.editableFields().contains("classId"));
        assertFalse(profile.editableFields().contains("admissionNo"));
        assertEquals("Class 5", profile.className());
        assertEquals("A", profile.sectionName());
    }

    @Test
    void updateProfileAllowsAdminToChangePersonalFieldsButNotAdmissionNo() {
        UUID studentId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        Student student = sampleStudent(studentId);
        when(studentRepository.findByIdAndSchoolId(studentId, schoolId)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentGuardianRepository.findWithGuardians(schoolId, studentId)).thenReturn(List.of());
        stubProfileLookups(studentId, yearId, classId, sectionId, student);
        when(academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId)).thenReturn(Optional.of(year(yearId)));

        StudentProfileUpdateRequest request = new StudentProfileUpdateRequest(
                "Aisha", "Khan", LocalDate.of(2014, 3, 2), "FEMALE", "A+", "Islam", "IN",
                "9991112222", "8881112222", "Old Town", "New Town", "City School",
                "Imran Khan", "Sara Khan", "7771112222", "6661112222",
                LocalDate.of(2025, 4, 1), classId, sectionId, 12);

        StudentProfileDto profile = studentService.updateProfile(studentId, request);

        assertEquals("Aisha", student.getFirstName());
        assertEquals("ADM100", student.getAdmissionNo());
        assertEquals("9991112222", student.getPhone());
        assertTrue(profile.canEdit());
        verify(guardianRepository, org.mockito.Mockito.atLeastOnce()).save(any(Guardian.class));
    }

    @Test
    void updateProfileAllowsClassTeacherLimitedFields() {
        TestSecurity.loginAsTeacher();
        UUID studentId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        Student student = sampleStudent(studentId);
        student.setPhone("111");
        when(studentRepository.findByIdAndSchoolId(studentId, schoolId)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentGuardianRepository.findWithGuardians(schoolId, studentId)).thenReturn(List.of());
        stubEnrollment(studentId, yearId, classId, sectionId);
        stubClassTeacher(sectionId, yearId);
        when(academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId)).thenReturn(Optional.of(year(yearId)));
        when(academicYearRepository.findByIdAndSchoolId(yearId, schoolId)).thenReturn(Optional.of(year(yearId)));
        when(attendanceService.getStudentSummary(any(), any(), any()))
                .thenReturn(new AttendanceSummaryDto(studentId, "Meera Sharma", 0, 0, 0, 0, 0, 0));
        when(classRepository.findByIdAndSchoolId(classId, schoolId)).thenReturn(Optional.of(schoolClass(classId)));
        when(sectionRepository.findByIdAndSchoolId(sectionId, schoolId)).thenReturn(Optional.of(section(sectionId, classId)));

        StudentEnrollment existing = new StudentEnrollment();
        existing.setStudentId(studentId);
        existing.setAcademicYearId(yearId);
        existing.setSectionId(sectionId);
        existing.setRollNumber(4);
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(studentId, yearId))
                .thenReturn(Optional.of(existing));
        when(enrollmentRepository.save(any(StudentEnrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentProfileUpdateRequest request = new StudentProfileUpdateRequest(
                "Meera", "Sharma", LocalDate.of(2014, 5, 10), "FEMALE", "O+", "Hindu", "PK",
                "1234567890", "0987654321", "Should stay", "Should stay", "Should stay",
                "Ravi Sharma", "Anita Sharma", "111", "222",
                LocalDate.of(2024, 1, 1), classId, sectionId, 9);

        studentService.updateProfile(studentId, request);

        assertEquals("1234567890", student.getPhone());
        assertEquals("0987654321", student.getEmergencyContact());
        assertEquals(null, student.getPermanentAddress());
        assertEquals(null, student.getPreviousSchool());
        assertEquals("Hindu", student.getReligion());
        assertEquals(9, existing.getRollNumber());
    }

    @Test
    void updateProfileRejectsGeneralTeacher() {
        TestSecurity.loginAsTeacher();
        UUID studentId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        Student student = sampleStudent(studentId);
        when(studentRepository.findByIdAndSchoolId(studentId, schoolId)).thenReturn(Optional.of(student));
        stubEnrollment(studentId, yearId, classId, sectionId);
        when(teacherRepository.findBySchoolIdAndUserId(schoolId, TestSecurity.USER_ID)).thenReturn(Optional.empty());

        StudentProfileUpdateRequest request = new StudentProfileUpdateRequest(
                "Meera", "Sharma", LocalDate.of(2014, 5, 10), "FEMALE", null, null, null,
                "123", null, null, null, null, null, null, null, null, null, classId, sectionId, 1);

        assertThrows(AuthException.class, () -> studentService.updateProfile(studentId, request));
        verify(studentRepository, never()).save(any());
    }

    private Student sampleStudent(UUID studentId) {
        Student student = new Student();
        student.setId(studentId);
        student.setSchoolId(schoolId);
        student.setAdmissionNo("ADM100");
        student.setFirstName("Meera");
        student.setLastName("Sharma");
        student.setReligion("Hindu");
        student.setStatus(StudentStatus.ACTIVE);
        student.setAdmissionDate(LocalDate.of(2025, 4, 1));
        return student;
    }

    private void stubProfileLookups(UUID studentId, UUID yearId, UUID classId, UUID sectionId, Student student) {
        stubEnrollment(studentId, yearId, classId, sectionId);
        when(studentGuardianRepository.findWithGuardians(schoolId, studentId)).thenReturn(List.of());
        when(academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId)).thenReturn(Optional.of(year(yearId)));
        when(academicYearRepository.findByIdAndSchoolId(yearId, schoolId)).thenReturn(Optional.of(year(yearId)));
        when(sectionRepository.findByIdAndSchoolId(sectionId, schoolId)).thenReturn(Optional.of(section(sectionId, classId)));
        when(classRepository.findByIdAndSchoolId(classId, schoolId)).thenReturn(Optional.of(schoolClass(classId)));
        when(attendanceService.getStudentSummary(any(), any(), any()))
                .thenReturn(new AttendanceSummaryDto(studentId, student.getDisplayName(), 10, 1, 0, 0, 11, 90.9));
    }

    private void stubEnrollment(UUID studentId, UUID yearId, UUID classId, UUID sectionId) {
        AcademicYear year = year(yearId);
        when(academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId)).thenReturn(Optional.of(year));
        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudentId(studentId);
        enrollment.setAcademicYearId(yearId);
        enrollment.setSectionId(sectionId);
        enrollment.setRollNumber(4);
        enrollment.setEnrollmentDate(LocalDate.of(2025, 4, 1));
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(studentId, yearId)).thenReturn(Optional.of(enrollment));
        when(sectionRepository.findByIdAndSchoolId(sectionId, schoolId)).thenReturn(Optional.of(section(sectionId, classId)));
        when(classRepository.findByIdAndSchoolId(classId, schoolId)).thenReturn(Optional.of(schoolClass(classId)));
        when(academicYearRepository.findByIdAndSchoolId(yearId, schoolId)).thenReturn(Optional.of(year));
    }

    private void stubClassTeacher(UUID sectionId, UUID yearId) {
        TeacherProfile teacher = new TeacherProfile();
        teacher.setId(UUID.randomUUID());
        teacher.setUserId(TestSecurity.USER_ID);
        teacher.setSchoolId(schoolId);
        when(teacherRepository.findBySchoolIdAndUserId(schoolId, TestSecurity.USER_ID)).thenReturn(Optional.of(teacher));
        TeacherSection assignment = new TeacherSection();
        assignment.setTeacherId(teacher.getId());
        assignment.setSectionId(sectionId);
        assignment.setAcademicYearId(yearId);
        assignment.setClassTeacher(true);
        when(teacherSectionRepository.findByTeacherIdAndSchoolId(teacher.getId(), schoolId))
                .thenReturn(List.of(assignment));
    }

    private AcademicYear year(UUID yearId) {
        AcademicYear year = new AcademicYear();
        year.setId(yearId);
        year.setSchoolId(schoolId);
        year.setName("2025-26");
        year.setStartDate(LocalDate.of(2025, 4, 1));
        year.setEndDate(LocalDate.of(2026, 3, 31));
        year.setCurrent(true);
        return year;
    }

    private Section section(UUID sectionId, UUID classId) {
        Section section = new Section();
        section.setId(sectionId);
        section.setClassId(classId);
        section.setName("A");
        return section;
    }

    private SchoolClass schoolClass(UUID classId) {
        SchoolClass klass = new SchoolClass();
        klass.setId(classId);
        klass.setName("Class 5");
        return klass;
    }
}
