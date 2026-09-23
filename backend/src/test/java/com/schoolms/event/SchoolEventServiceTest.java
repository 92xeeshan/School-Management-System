package com.schoolms.event;

import com.schoolms.TestSecurity;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.SchoolClass;
import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.academics.Section;
import com.schoolms.academics.SectionRepository;
import com.schoolms.academics.StudentEnrollmentRepository;
import com.schoolms.academics.TeacherProfileRepository;
import com.schoolms.academics.TeacherSectionRepository;
import com.schoolms.common.enums.EventType;
import com.schoolms.common.enums.EventVisibility;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.event.dto.SchoolEventDto;
import com.schoolms.event.dto.SchoolEventRequest;
import com.schoolms.school.School;
import com.schoolms.school.SchoolRepository;
import com.schoolms.student.GuardianRepository;
import com.schoolms.student.StudentGuardianRepository;
import com.schoolms.student.StudentRepository;
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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
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
    @Mock private SchoolRepository schoolRepository;
    @Mock private HolidayPdfService holidayPdfService;

    private SchoolEventService service;

    @BeforeEach
    void setUp() {
        service = new SchoolEventService(eventRepository, classRepository, sectionRepository,
                teacherRepository, teacherSectionRepository, studentRepository, enrollmentRepository,
                academicYearRepository, guardianRepository, studentGuardianRepository,
                schoolRepository, holidayPdfService);
        lenient().when(teacherRepository.findBySchoolIdAndUserId(eq(TestSecurity.SCHOOL_ID), any()))
                .thenReturn(Optional.empty());
        lenient().when(studentRepository.findBySchoolIdAndUserId(eq(TestSecurity.SCHOOL_ID), any()))
                .thenReturn(Optional.empty());
        lenient().when(guardianRepository.findBySchoolIdAndUserId(eq(TestSecurity.SCHOOL_ID), any()))
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

        List<SchoolEventDto> result = service.list(LocalDate.now(), LocalDate.now().plusDays(7), null);

        assertEquals(1, result.size());
        assertEquals("Sports Day", result.get(0).title());
    }

    @Test
    void staffSeesStaffAndSchoolWideEvents() {
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("ADMIN"),
                Set.of("EVENT_READ", "EVENT_MANAGE"));
        when(eventRepository.findInRange(eq(TestSecurity.SCHOOL_ID), any(), any()))
                .thenReturn(List.of(schoolWide("Sports Day"), staffOnly("Board Review")));

        List<SchoolEventDto> result = service.list(LocalDate.now(), LocalDate.now().plusDays(7), null);

        assertEquals(2, result.size());
    }

    @Test
    void classScopedEventHiddenFromOtherSection() {
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("STUDENT"),
                Set.of("EVENT_READ"));
        UUID otherSection = UUID.randomUUID();
        SchoolEvent scoped = event("Class picnic", EventVisibility.CLASS_WIDE);
        scoped.setClassId(UUID.randomUUID());
        scoped.setSectionId(otherSection);
        when(eventRepository.findInRange(eq(TestSecurity.SCHOOL_ID), any(), any()))
                .thenReturn(List.of(schoolWide("Sports Day"), scoped));

        List<SchoolEventDto> result = service.list(LocalDate.now(), LocalDate.now().plusDays(7), null);

        assertEquals(1, result.size());
        assertEquals("Sports Day", result.get(0).title());
    }

    @Test
    void parentSeesRoleScopedParentEvents() {
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("PARENT"),
                Set.of("EVENT_READ"));
        SchoolEvent parentEvent = event("Parent orientation", EventVisibility.ROLE);
        parentEvent.setAudienceRole("PARENT");
        SchoolEvent teacherEvent = event("Teacher briefing", EventVisibility.ROLE);
        teacherEvent.setAudienceRole("TEACHER");
        when(eventRepository.findInRange(eq(TestSecurity.SCHOOL_ID), any(), any()))
                .thenReturn(List.of(parentEvent, teacherEvent, schoolWide("Sports Day")));

        List<SchoolEventDto> result = service.list(LocalDate.now(), LocalDate.now().plusDays(7), null);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(e -> e.title().equals("Parent orientation")));
        assertTrue(result.stream().anyMatch(e -> e.title().equals("Sports Day")));
    }

    @Test
    void typeFilterKeepsOnlyHolidays() {
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("ADMIN"),
                Set.of("EVENT_READ", "EVENT_MANAGE"));
        SchoolEvent holiday = event("Founders Day", EventVisibility.SCHOOL_WIDE);
        holiday.setEventType(EventType.HOLIDAY);
        when(eventRepository.findInRange(eq(TestSecurity.SCHOOL_ID), any(), any()))
                .thenReturn(List.of(holiday, schoolWide("Sports Day")));

        List<SchoolEventDto> result = service.list(LocalDate.now(), LocalDate.now().plusDays(7), "HOLIDAY");

        assertEquals(1, result.size());
        assertEquals("Founders Day", result.get(0).title());
    }

    @Test
    void upcomingCapsAtFiveEvents() {
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("ADMIN"),
                Set.of("EVENT_READ", "EVENT_MANAGE"));
        when(eventRepository.findInRange(eq(TestSecurity.SCHOOL_ID), any(), any()))
                .thenReturn(List.of(
                        schoolWide("One"), schoolWide("Two"), schoolWide("Three"),
                        schoolWide("Four"), schoolWide("Five"), schoolWide("Six")));

        List<SchoolEventDto> result = service.upcoming(30);

        assertEquals(5, result.size());
    }

    @Test
    void createPersistsClassScopedEvent() {
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("ADMIN"),
                Set.of("EVENT_READ", "EVENT_MANAGE"));
        UUID classId = UUID.fromString("30000000-0000-0000-0000-000000000011");
        UUID sectionId = UUID.fromString("30000000-0000-0000-0000-000000000021");
        SchoolClass klass = new SchoolClass();
        klass.setId(classId);
        klass.setName("Class 5");
        Section section = new Section();
        section.setId(sectionId);
        section.setClassId(classId);
        section.setName("A");
        when(classRepository.findByIdAndSchoolId(classId, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(klass));
        when(sectionRepository.findByIdAndSchoolId(sectionId, TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(section));
        when(eventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SchoolEventRequest request = new SchoolEventRequest(
                "Class picnic", "Bring water bottles", "SPORTS",
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(3),
                true, null, null, "Ground", "CLASS_WIDE", null, classId, sectionId);

        SchoolEventDto created = service.create(request);

        assertEquals("Class picnic", created.title());
        assertEquals("SPORTS", created.eventType());
        assertEquals(classId, created.classId());
        assertEquals(sectionId, created.sectionId());
        ArgumentCaptor<SchoolEvent> captor = ArgumentCaptor.forClass(SchoolEvent.class);
        verify(eventRepository).save(captor.capture());
        assertEquals(EventVisibility.CLASS_WIDE, captor.getValue().getVisibilityScope());
    }

    @Test
    void exportHolidaysUsesOnlyHolidayType() {
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("ADMIN"),
                Set.of("EVENT_READ", "EVENT_MANAGE"));
        SchoolEvent holiday = event("Founders Day", EventVisibility.SCHOOL_WIDE);
        holiday.setEventType(EventType.HOLIDAY);
        when(eventRepository.findInRange(eq(TestSecurity.SCHOOL_ID), any(), any()))
                .thenReturn(List.of(holiday, schoolWide("Sports Day")));
        School school = new School();
        school.setName("Demo Public School");
        when(schoolRepository.findById(TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(school));
        when(holidayPdfService.render(eq(school), eq(LocalDate.now().getYear()), any()))
                .thenReturn(new byte[] {1, 2, 3});

        byte[] pdf = service.exportHolidays(null);

        assertEquals(3, pdf.length);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SchoolEventDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(holidayPdfService).render(eq(school), eq(LocalDate.now().getYear()), captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals("Founders Day", captor.getValue().get(0).title());
    }

    @Test
    void invalidDateRangeRejected() {
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("ADMIN"),
                Set.of("EVENT_READ", "EVENT_MANAGE"));
        assertThrows(BusinessException.class,
                () -> service.list(LocalDate.now().plusDays(3), LocalDate.now(), null));
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
