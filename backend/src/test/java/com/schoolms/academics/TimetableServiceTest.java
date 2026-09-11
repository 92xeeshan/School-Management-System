package com.schoolms.academics;

import com.schoolms.TestSecurity;
import com.schoolms.academics.dto.TimetableEntryRequest;
import com.schoolms.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimetableServiceTest {

    @Mock
    private TimetableEntryRepository timetableRepository;
    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private SchoolClassRepository classRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private TeacherProfileRepository teacherRepository;
    @Mock
    private AcademicYearRepository academicYearRepository;

    private TimetableService timetableService;

    @BeforeEach
    void setUp() {
        timetableService = new TimetableService(timetableRepository, sectionRepository, classRepository,
                subjectRepository, teacherRepository, academicYearRepository);
        TestSecurity.loginAsAdmin();
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void createRejectsTeacherConflict() {
        UUID sectionId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        stubValidRefs(sectionId, yearId, teacherId);
        when(timetableRepository.existsBySectionIdAndAcademicYearIdAndDayOfWeekAndPeriodNumber(
                sectionId, yearId, (short) 1, (short) 1)).thenReturn(false);
        when(timetableRepository.existsByTeacherIdAndAcademicYearIdAndDayOfWeekAndPeriodNumber(
                teacherId, yearId, (short) 1, (short) 1)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> timetableService.create(request(sectionId, yearId, teacherId, "Room 1")));
        assertEquals("timetable.teacher_conflict", ex.getCode());
        verify(timetableRepository, never()).save(any());
    }

    @Test
    void createRejectsRoomConflict() {
        UUID sectionId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        stubValidRefs(sectionId, yearId, teacherId);
        when(timetableRepository.existsBySectionIdAndAcademicYearIdAndDayOfWeekAndPeriodNumber(
                sectionId, yearId, (short) 1, (short) 1)).thenReturn(false);
        when(timetableRepository.existsByTeacherIdAndAcademicYearIdAndDayOfWeekAndPeriodNumber(
                teacherId, yearId, (short) 1, (short) 1)).thenReturn(false);
        when(timetableRepository.existsBySchoolIdAndAcademicYearIdAndDayOfWeekAndPeriodNumberAndRoomIgnoreCase(
                TestSecurity.SCHOOL_ID, yearId, (short) 1, (short) 1, "Room 1")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> timetableService.create(request(sectionId, yearId, teacherId, "Room 1")));
        assertEquals("timetable.room_conflict", ex.getCode());
        verify(timetableRepository, never()).save(any());
    }

    @Test
    void createPersistsEntry() {
        UUID sectionId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        Section section = new Section();
        section.setId(sectionId);
        section.setName("A");
        section.setClassId(classId);
        when(sectionRepository.findByIdAndSchoolId(sectionId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(section));
        when(academicYearRepository.findByIdAndSchoolId(yearId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(new AcademicYear()));
        TeacherProfile teacher = new TeacherProfile();
        teacher.setId(teacherId);
        teacher.setFirstName("Asha");
        teacher.setLastName("Sharma");
        when(teacherRepository.findByIdAndSchoolId(teacherId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(teacher));
        when(timetableRepository.existsBySectionIdAndAcademicYearIdAndDayOfWeekAndPeriodNumber(
                sectionId, yearId, (short) 1, (short) 1)).thenReturn(false);
        when(timetableRepository.existsByTeacherIdAndAcademicYearIdAndDayOfWeekAndPeriodNumber(
                teacherId, yearId, (short) 1, (short) 1)).thenReturn(false);
        when(timetableRepository.existsBySchoolIdAndAcademicYearIdAndDayOfWeekAndPeriodNumberAndRoomIgnoreCase(
                TestSecurity.SCHOOL_ID, yearId, (short) 1, (short) 1, "Room 1")).thenReturn(false);
        when(timetableRepository.save(any(TimetableEntry.class))).thenAnswer(invocation -> {
            TimetableEntry entry = invocation.getArgument(0);
            entry.setId(UUID.randomUUID());
            return entry;
        });
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(classId);
        schoolClass.setName("Class 8");
        when(classRepository.findByIdAndSchoolId(classId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(schoolClass));

        var dto = timetableService.create(request(sectionId, yearId, teacherId, "Room 1"));

        assertEquals("Class 8", dto.className());
        assertEquals("A", dto.sectionName());
        assertEquals("Asha Sharma", dto.teacherName());
        assertEquals("Room 1", dto.room());
        verify(timetableRepository).save(any(TimetableEntry.class));
    }

    private void stubValidRefs(UUID sectionId, UUID yearId, UUID teacherId) {
        when(sectionRepository.findByIdAndSchoolId(sectionId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(new Section()));
        when(academicYearRepository.findByIdAndSchoolId(yearId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(new AcademicYear()));
        when(teacherRepository.findByIdAndSchoolId(teacherId, TestSecurity.SCHOOL_ID))
                .thenReturn(java.util.Optional.of(new TeacherProfile()));
    }

    private TimetableEntryRequest request(UUID sectionId, UUID yearId, UUID teacherId, String room) {
        return new TimetableEntryRequest(
                sectionId, yearId, (short) 1, (short) 1,
                LocalTime.of(9, 0), LocalTime.of(9, 45),
                null, teacherId, room);
    }
}
