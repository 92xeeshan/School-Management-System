package com.schoolms.exam;

import com.schoolms.TestSecurity;
import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.ClassSubject;
import com.schoolms.academics.ClassSubjectRepository;
import com.schoolms.academics.SchoolClass;
import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.academics.Section;
import com.schoolms.academics.SectionRepository;
import com.schoolms.academics.Subject;
import com.schoolms.academics.SubjectRepository;
import com.schoolms.academics.TeacherProfile;
import com.schoolms.academics.TeacherProfileRepository;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.exam.dto.ExamScheduleDto;
import com.schoolms.exam.dto.ExamScheduleRequest;
import com.schoolms.exam.dto.ScheduleConflictDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamScheduleServiceTest {

    @Mock private ExamScheduleRepository scheduleRepository;
    @Mock private ExamScheduleInvigilatorRepository invigilatorRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private SchoolClassRepository classRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private ClassSubjectRepository classSubjectRepository;
    @Mock private TeacherProfileRepository teacherRepository;

    private ExamScheduleService service;

    private final UUID yearId = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private final UUID classId = UUID.fromString("30000000-0000-0000-0000-000000000011");
    private final UUID sectionId = UUID.fromString("30000000-0000-0000-0000-000000000021");
    private final UUID subjectId = UUID.fromString("30000000-0000-0000-0000-000000000031");
    private final UUID teacherId = UUID.fromString("30000000-0000-0000-0000-000000000041");

    @BeforeEach
    void setUp() {
        service = new ExamScheduleService(scheduleRepository, invigilatorRepository, academicYearRepository,
                classRepository, sectionRepository, subjectRepository, classSubjectRepository, teacherRepository);
        TestSecurity.loginAsAdmin();
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void createPersistsDraftSchedule() {
        stubValidRefs();
        stubTeacher();
        when(scheduleRepository.findBySchoolIdAndExamDate(TestSecurity.SCHOOL_ID, LocalDate.of(2026, 3, 20)))
                .thenReturn(List.of());
        when(scheduleRepository.existsBySchoolIdAndAcademicYearIdAndSectionIdAndSubjectIdAndExamTerm(
                TestSecurity.SCHOOL_ID, yearId, sectionId, subjectId, "TERM")).thenReturn(false);
        when(scheduleRepository.save(any(ExamSchedule.class))).thenAnswer(inv -> {
            ExamSchedule schedule = inv.getArgument(0);
            if (schedule.getId() == null) {
                schedule.setId(UUID.randomUUID());
            }
            return schedule;
        });
        stubLookup();

        ExamScheduleDto dto = service.create(request(sectionId, subjectId, "Room 201", List.of(teacherId)));

        assertEquals("DRAFT", dto.status());
        assertEquals("Room 201", dto.room());
        verify(scheduleRepository).save(any(ExamSchedule.class));
        verify(invigilatorRepository).save(any(ExamScheduleInvigilator.class));
    }

    @Test
    void createRejectsRoomOverlap() {
        stubValidRefs();
        ExamSchedule existing = overlapping("Room 201", UUID.randomUUID());
        when(scheduleRepository.findBySchoolIdAndExamDate(TestSecurity.SCHOOL_ID, LocalDate.of(2026, 3, 20)))
                .thenReturn(List.of(existing));
        when(invigilatorRepository.findByExamScheduleIdIn(List.of(existing.getId()))).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(request(sectionId, subjectId, "Room 201", List.of())));
        assertEquals("exam_schedule.room_overlap", ex.getCode());
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void createRejectsSectionOverlap() {
        stubValidRefs();
        ExamSchedule existing = overlapping("Hall 1", sectionId);
        when(scheduleRepository.findBySchoolIdAndExamDate(TestSecurity.SCHOOL_ID, LocalDate.of(2026, 3, 20)))
                .thenReturn(List.of(existing));
        when(invigilatorRepository.findByExamScheduleIdIn(List.of(existing.getId()))).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(request(sectionId, subjectId, "Hall 2", List.of())));
        assertEquals("exam_schedule.section_overlap", ex.getCode());
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void createRejectsInvigilatorOverlap() {
        stubValidRefs();
        stubTeacher();
        ExamSchedule existing = overlapping("Hall 1", UUID.randomUUID());
        ExamScheduleInvigilator link = new ExamScheduleInvigilator();
        link.setExamScheduleId(existing.getId());
        link.setTeacherId(teacherId);
        when(scheduleRepository.findBySchoolIdAndExamDate(TestSecurity.SCHOOL_ID, LocalDate.of(2026, 3, 20)))
                .thenReturn(List.of(existing));
        when(invigilatorRepository.findByExamScheduleIdIn(List.of(existing.getId()))).thenReturn(List.of(link));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(request(sectionId, subjectId, "Hall 2", List.of(teacherId))));
        assertEquals("exam_schedule.invigilator_overlap", ex.getCode());
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void validateReportsRoomConflictWithoutSaving() {
        stubValidRefs();
        ExamSchedule existing = overlapping("Room 201", UUID.randomUUID());
        when(scheduleRepository.findBySchoolIdAndExamDate(TestSecurity.SCHOOL_ID, LocalDate.of(2026, 3, 20)))
                .thenReturn(List.of(existing));
        when(invigilatorRepository.findByExamScheduleIdIn(List.of(existing.getId()))).thenReturn(List.of());

        var result = service.validate(request(sectionId, subjectId, "Room 201", List.of()), null);

        assertFalse(result.valid());
        assertEquals("exam_schedule.room_overlap", result.conflicts().get(0).code());
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void findConflictsIgnoresNonOverlappingTimes() {
        ExamSchedule existing = overlapping("Room 201", sectionId);
        existing.setStartTime(LocalTime.of(13, 0));
        existing.setEndTime(LocalTime.of(15, 0));
        when(scheduleRepository.findBySchoolIdAndExamDate(TestSecurity.SCHOOL_ID, LocalDate.of(2026, 3, 20)))
                .thenReturn(List.of(existing));
        when(invigilatorRepository.findByExamScheduleIdIn(List.of(existing.getId()))).thenReturn(List.of());

        List<ScheduleConflictDto> conflicts = service.findConflicts(
                request(sectionId, subjectId, "Room 201", List.of(teacherId)),
                TestSecurity.SCHOOL_ID, null);

        assertTrue(conflicts.isEmpty());
    }

    private void stubValidRefs() {
        AcademicYear year = new AcademicYear();
        year.setId(yearId);
        year.setName("2025-26");
        when(academicYearRepository.findByIdAndSchoolId(yearId, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(year));
        SchoolClass klass = new SchoolClass();
        klass.setId(classId);
        klass.setName("Class 5");
        when(classRepository.findByIdAndSchoolId(classId, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(klass));
        Section section = new Section();
        section.setId(sectionId);
        section.setClassId(classId);
        section.setName("A");
        when(sectionRepository.findByIdAndSchoolId(sectionId, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(section));
        Subject subject = new Subject();
        subject.setId(subjectId);
        subject.setName("Mathematics");
        when(subjectRepository.findByIdAndSchoolId(subjectId, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(subject));
        ClassSubject link = new ClassSubject();
        link.setClassId(classId);
        link.setSubjectId(subjectId);
        when(classSubjectRepository.findByClassIdAndSchoolId(classId, TestSecurity.SCHOOL_ID)).thenReturn(List.of(link));
    }

    private void stubTeacher() {
        TeacherProfile teacher = new TeacherProfile();
        teacher.setId(teacherId);
        teacher.setFirstName("Asha");
        teacher.setLastName("Sharma");
        when(teacherRepository.findByIdAndSchoolId(teacherId, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(teacher));
    }

    private void stubLookup() {
        AcademicYear year = new AcademicYear();
        year.setId(yearId);
        year.setName("2025-26");
        SchoolClass klass = new SchoolClass();
        klass.setId(classId);
        klass.setName("Class 5");
        Section section = new Section();
        section.setId(sectionId);
        section.setClassId(classId);
        section.setName("A");
        Subject subject = new Subject();
        subject.setId(subjectId);
        subject.setName("Mathematics");
        TeacherProfile teacher = new TeacherProfile();
        teacher.setId(teacherId);
        teacher.setFirstName("Asha");
        teacher.setLastName("Sharma");
        when(academicYearRepository.findBySchoolIdOrderByStartDateDesc(TestSecurity.SCHOOL_ID)).thenReturn(List.of(year));
        when(classRepository.findBySchoolIdOrderBySortOrderAsc(TestSecurity.SCHOOL_ID)).thenReturn(List.of(klass));
        when(sectionRepository.findBySchoolIdOrderByNameAsc(TestSecurity.SCHOOL_ID)).thenReturn(List.of(section));
        when(subjectRepository.findBySchoolIdOrderByNameAsc(TestSecurity.SCHOOL_ID)).thenReturn(List.of(subject));
        when(teacherRepository.findBySchoolIdOrderByFirstNameAsc(TestSecurity.SCHOOL_ID)).thenReturn(List.of(teacher));
        when(invigilatorRepository.findByExamScheduleIdIn(any())).thenReturn(List.of());
        doNothing().when(invigilatorRepository).deleteByExamScheduleId(any());
    }

    private ExamSchedule overlapping(String room, UUID existingSectionId) {
        ExamSchedule schedule = new ExamSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setSchoolId(TestSecurity.SCHOOL_ID);
        schedule.setSectionId(existingSectionId);
        schedule.setExamDate(LocalDate.of(2026, 3, 20));
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(11, 0));
        schedule.setRoom(room);
        schedule.setStatus("DRAFT");
        return schedule;
    }

    private ExamScheduleRequest request(UUID section, UUID subject, String room, List<UUID> invigilators) {
        return new ExamScheduleRequest(
                yearId, classId, section, subject, "TERM",
                LocalDate.of(2026, 3, 20), LocalTime.of(9, 0), LocalTime.of(11, 0),
                room, new BigDecimal("100"), new BigDecimal("33"), invigilators, "DRAFT");
    }
}
