package com.schoolms.staff;

import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.SchoolClass;
import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.academics.Section;
import com.schoolms.academics.SectionRepository;
import com.schoolms.academics.TeacherProfile;
import com.schoolms.academics.TeacherProfileRepository;
import com.schoolms.academics.TeacherSection;
import com.schoolms.academics.TeacherSectionRepository;
import com.schoolms.academics.TeacherSubjectRepository;
import com.schoolms.common.enums.EmploymentType;
import com.schoolms.common.enums.Gender;
import com.schoolms.common.enums.StaffType;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.staff.dto.ClassTeacherRequest;
import com.schoolms.staff.dto.StaffMemberDto;
import com.schoolms.staff.dto.StaffMemberRequest;
import com.schoolms.TestSecurity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class StaffServiceTest {

    @Mock private TeacherProfileRepository teacherRepository;
    @Mock private TeacherSubjectRepository teacherSubjectRepository;
    @Mock private TeacherSectionRepository teacherSectionRepository;
    @Mock private NonTeachingStaffRepository nonTeachingStaffRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private SchoolClassRepository classRepository;
    @Mock private AcademicYearRepository academicYearRepository;

    private StaffService staffService;

    private final UUID schoolId = TestSecurity.SCHOOL_ID;
    private final UUID teacherId = UUID.fromString("30000000-0000-0000-0000-000000000041");

    @BeforeEach
    void setUp() {
        staffService = new StaffService(teacherRepository, teacherSubjectRepository,
                teacherSectionRepository, nonTeachingStaffRepository, sectionRepository,
                classRepository, academicYearRepository);
        TestSecurity.loginAsAdmin();
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void createTeachingStaffPersistsWithSchoolContext() {
        when(teacherRepository.existsBySchoolIdAndEmployeeNo(schoolId, "EMP100")).thenReturn(false);
        when(teacherRepository.save(any(TeacherProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffMemberDto dto = staffService.createTeachingStaff(request("EMP100"));

        ArgumentCaptor<TeacherProfile> captor = ArgumentCaptor.forClass(TeacherProfile.class);
        verify(teacherRepository).save(captor.capture());
        TeacherProfile saved = captor.getValue();
        assertEquals(schoolId, saved.getSchoolId());
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals("EMP100", saved.getEmployeeNo());
        assertEquals(EmploymentType.FULL_TIME, saved.getEmploymentType());
        assertEquals(StaffType.TEACHING, dto.staffType());
    }

    @Test
    void createTeachingStaffRejectsDuplicateEmployeeNo() {
        when(teacherRepository.existsBySchoolIdAndEmployeeNo(schoolId, "EMP100")).thenReturn(true);

        assertThrows(BusinessException.class, () -> staffService.createTeachingStaff(request("EMP100")));
        verify(teacherRepository, never()).save(any(TeacherProfile.class));
    }

    @Test
    void deactivateTeachingStaffSetsInactiveAndClearsClassTeacher() {
        TeacherProfile teacher = teacher(teacherId);
        when(teacherRepository.findByIdAndSchoolId(teacherId, schoolId)).thenReturn(Optional.of(teacher));
        TeacherSection link = classTeacherLink(teacherId, UUID.randomUUID());
        when(teacherSectionRepository.findByTeacherIdAndSchoolId(teacherId, schoolId))
                .thenReturn(List.of(link));

        staffService.deactivateTeachingStaff(teacherId);

        assertEquals("INACTIVE", teacher.getStatus());
        verify(teacherRepository).save(teacher);
        assertFalse(link.isClassTeacher());
        verify(teacherSectionRepository).save(link);
    }

    @Test
    void assignClassTeacherMarksSectionAsClassTeacher() {
        UUID sectionId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        TeacherProfile teacher = teacher(teacherId);
        AcademicYear year = new AcademicYear();
        year.setId(yearId);
        Section section = new Section();
        section.setId(sectionId);
        section.setSchoolId(schoolId);
        section.setClassId(UUID.randomUUID());
        section.setName("A");

        when(teacherRepository.findByIdAndSchoolId(teacherId, schoolId)).thenReturn(Optional.of(teacher));
        when(academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId)).thenReturn(Optional.of(year));
        when(teacherSectionRepository.findByTeacherIdAndSchoolId(teacherId, schoolId)).thenReturn(List.of());
        when(sectionRepository.findByIdAndSchoolId(sectionId, schoolId)).thenReturn(Optional.of(section));
        when(teacherSectionRepository.findBySectionIdAndSchoolId(sectionId, schoolId)).thenReturn(List.of());
        when(teacherSectionRepository.findByTeacherIdAndSectionIdAndAcademicYearId(teacherId, sectionId, yearId))
                .thenReturn(Optional.empty());
        when(teacherSectionRepository.save(any(TeacherSection.class))).thenAnswer(inv -> inv.getArgument(0));
        when(teacherSubjectRepository.findByTeacherIdAndSchoolId(teacherId, schoolId)).thenReturn(List.of());
        when(classRepository.findBySchoolIdOrderBySortOrderAsc(schoolId)).thenReturn(List.of(schoolClass()));
        when(sectionRepository.findBySchoolIdOrderByNameAsc(schoolId)).thenReturn(List.of(section));
        when(teacherRepository.findBySchoolIdOrderByFirstNameAsc(schoolId)).thenReturn(List.of(teacher));
        when(teacherSectionRepository.findBySchoolId(schoolId)).thenReturn(List.of());

        staffService.assignClassTeacher(teacherId, new ClassTeacherRequest(sectionId));

        ArgumentCaptor<TeacherSection> captor = ArgumentCaptor.forClass(TeacherSection.class);
        verify(teacherSectionRepository).save(captor.capture());
        TeacherSection saved = captor.getValue();
        assertEquals(sectionId, saved.getSectionId());
        assertEquals(yearId, saved.getAcademicYearId());
        assertTrue(saved.isClassTeacher());
    }

    @Test
    void createNonTeachingStaffPersistsWithSchoolContext() {
        when(nonTeachingStaffRepository.existsBySchoolIdAndEmployeeNo(schoolId, "EMP100")).thenReturn(false);
        when(nonTeachingStaffRepository.save(any(NonTeachingStaff.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        StaffMemberDto dto = staffService.createNonTeachingStaff(request("EMP100"));

        ArgumentCaptor<NonTeachingStaff> captor = ArgumentCaptor.forClass(NonTeachingStaff.class);
        verify(nonTeachingStaffRepository).save(captor.capture());
        assertEquals(schoolId, captor.getValue().getSchoolId());
        assertEquals("ACTIVE", captor.getValue().getStatus());
        assertEquals(StaffType.NON_TEACHING, dto.staffType());
    }

    @Test
    void updateNonTeachingStaffRejectsDuplicateEmployeeNo() {
        NonTeachingStaff staff = new NonTeachingStaff();
        staff.setId(UUID.randomUUID());
        staff.setSchoolId(schoolId);
        staff.setEmployeeNo("EMP001");
        when(nonTeachingStaffRepository.findByIdAndSchoolId(staff.getId(), schoolId))
                .thenReturn(Optional.of(staff));
        when(nonTeachingStaffRepository.existsBySchoolIdAndEmployeeNoAndIdNot(schoolId, "EMP100", staff.getId()))
                .thenReturn(true);

        assertThrows(BusinessException.class,
                () -> staffService.updateNonTeachingStaff(staff.getId(), request("EMP100")));
        verify(nonTeachingStaffRepository, never()).save(any(NonTeachingStaff.class));
    }

    private StaffMemberRequest request(String employeeNo) {
        return new StaffMemberRequest(employeeNo, "Test", "Staff", null, null, Gender.MALE, null,
                "Senior Teacher", "Science", "M.Sc", EmploymentType.FULL_TIME, null, null, null);
    }

    private TeacherProfile teacher(UUID id) {
        TeacherProfile teacher = new TeacherProfile();
        teacher.setId(id);
        teacher.setSchoolId(schoolId);
        teacher.setEmployeeNo("EMP001");
        teacher.setFirstName("Asha");
        return teacher;
    }

    private TeacherSection classTeacherLink(UUID teacherId, UUID sectionId) {
        TeacherSection link = new TeacherSection();
        link.setId(UUID.randomUUID());
        link.setSchoolId(schoolId);
        link.setTeacherId(teacherId);
        link.setSectionId(sectionId);
        link.setAcademicYearId(UUID.randomUUID());
        link.setClassTeacher(true);
        return link;
    }

    private SchoolClass schoolClass() {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(UUID.randomUUID());
        schoolClass.setSchoolId(schoolId);
        schoolClass.setName("Class 5");
        return schoolClass;
    }
}
