package com.schoolms.event;

import com.schoolms.TestSecurity;
import com.schoolms.event.dto.EventDto;
import com.schoolms.event.dto.EventRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository);
        TestSecurity.loginAsAdmin();
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void createEventPersistsAndReturnsDto() {
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setSchoolId(TestSecurity.SCHOOL_ID);
        event.setTitle("Annual Day");
        event.setType("EXAM");
        event.setAudienceScope("ALL");
        event.setSource("MANUAL");
        event.setSyncStatus("NA");
        event.setStartDateTime(Instant.parse("2026-11-20T09:00:00Z"));
        event.setEndDateTime(Instant.parse("2026-11-20T17:00:00Z"));

        when(eventRepository.save(any(Event.class))).thenReturn(event);

        EventDto dto = eventService.create(new EventRequest(
                "Annual Day",
                "School celebration",
                "EXAM",
                Instant.parse("2026-11-20T09:00:00Z"),
                Instant.parse("2026-11-20T17:00:00Z"),
                false,
                "ALL",
                null,
                "MANUAL",
                "NA",
                null,
                null
        ));

        assertEquals("Annual Day", dto.title());
        assertEquals("EXAM", dto.type());
    }

    @Test
    void listVisibleReturnsSchoolEventsForAdmin() {
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setSchoolId(TestSecurity.SCHOOL_ID);
        event.setTitle("Holiday");
        event.setType("HOLIDAY");
        event.setAudienceScope("ALL");
        event.setSource("MANUAL");
        event.setSyncStatus("NA");
        event.setStartDateTime(Instant.parse("2026-10-02T00:00:00Z"));
        event.setEndDateTime(Instant.parse("2026-10-02T23:59:00Z"));

        when(eventRepository.findBySchoolIdOrderByStartDateTimeAsc(TestSecurity.SCHOOL_ID)).thenReturn(List.of(event));

        List<EventDto> items = eventService.listVisible(null, null, null);

        assertEquals(1, items.size());
        assertEquals("Holiday", items.get(0).title());
    }
}
