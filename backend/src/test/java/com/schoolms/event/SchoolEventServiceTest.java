package com.schoolms.event;

import com.schoolms.TestSecurity;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.academics.SectionRepository;
import com.schoolms.academics.StudentEnrollmentRepository;
import com.schoolms.academics.TeacherProfileRepository;
import com.schoolms.academics.TeacherSectionRepository;
import com.schoolms.common.enums.EventType;
import com.schoolms.common.enums.EventVisibility;
import com.schoolms.event.dto.SchoolEventDto;
import com.schoolms.student.GuardianRepository;
import com.schoolms.student.StudentGuardianRepository;
import com.schoolms.student.StudentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolEventServiceTest {

    @Mock private SchoolEventRepository eventRepository;
    @Mock private SchoolClassRepository classRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private TeacherProfileRepository teacherRepository;
    @Mock private TeacherSectionRepository teacherSectionRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private StudentEnrollmentRepository enrollmentRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private GuardianRepository guardianRepository;
    @Mock private StudentGuardianRepository studentGuardianRepository;

    private SchoolEventService service;

    @BeforeEach
    void setUp() {
        service = new SchoolEventService(eventRepository, classRepository, sectionRepository,
                teacherRepository, teacherSectionRepository, studentRepository, enrollmentRepository,
                academicYearRepository, guardianRepository, studentGuardianRepository);
        when(teacherRepository.findBySchoolIdAndUserId(eq(TestSecurity.SCHOOL_ID), any()))
                .thenReturn(Optional.empty());
        when(studentRepository.findBySchoolIdAndUserId(eq(TestSecurity.SCHOOL_ID), any()))
                .thenReturn(Optional.empty());
        when(guardianRepository.findBySchoolIdAndUserId(eq(TestSecurity.SCHOOL_ID), any()))
                .thenReturn(Optional.empty());
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void nonStaffOnlySeesSchoolWideEvents() {
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("STUDENT"),
                Set.of("EVENT_READ"));
        when(eventRepository.findInRange(eq(TestSecurity.SCHOOL_ID), any(), any()))
                .thenReturn(List.of(schoolWide("Sports Day"), staffOnly("Board Review")));

        List<SchoolEventDto> result = service.list(LocalDate.now(), LocalDate.now().plusDays(7));

        assertEquals(1, result.size());
        assertEquals("Sports Day", result.get(0).title());
    }

    @Test
    void staffSeesStaffAndSchoolWideEvents() {
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("ADMIN"),
                Set.of("EVENT_READ", "EVENT_MANAGE"));
        when(eventRepository.findInRange(eq(TestSecurity.SCHOOL_ID), any(), any()))
                .thenReturn(List.of(schoolWide("Sports Day"), staffOnly("Board Review")));

        List<SchoolEventDto> result = service.list(LocalDate.now(), LocalDate.now().plusDays(7));

        assertEquals(2, result.size());
    }

    private SchoolEvent schoolWide(String title) {
        return event(title, EventVisibility.SCHOOL_WIDE);
    }

    private SchoolEvent staffOnly(String title) {
        return event(title, EventVisibility.STAFF);
    }

    private SchoolEvent event(String title, EventVisibility visibility) {
        SchoolEvent event = new SchoolEvent();
        event.setId(UUID.randomUUID());
        event.setSchoolId(TestSecurity.SCHOOL_ID);
        event.setTitle(title);
        event.setEventType(EventType.EVENT);
        event.setVisibilityScope(visibility);
        event.setStartDate(LocalDate.now());
        return event;
    }
}
