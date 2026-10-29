package com.schoolms.leave;

import com.schoolms.common.enums.EventType;
import com.schoolms.event.SchoolEvent;
import com.schoolms.event.SchoolEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveDayCalculatorTest {

    private static final UUID SCHOOL = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Mock
    private SchoolEventRepository eventRepository;

    @Test
    void skipsWeekendsAndHolidays() {
        SchoolEvent holiday = new SchoolEvent();
        holiday.setEventType(EventType.HOLIDAY);
        holiday.setStartDate(LocalDate.of(2026, 4, 8));
        holiday.setEndDate(LocalDate.of(2026, 4, 8));
        when(eventRepository.findInRange(SCHOOL, LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 10)))
                .thenReturn(List.of(holiday));

        LeaveDayCalculator calculator = new LeaveDayCalculator(eventRepository);

        // Mon 6, Tue 7, Wed 8 holiday, Thu 9, Fri 10; Sat/Sun none in range
        assertEquals(new BigDecimal("4"), calculator.countWorkingDays(
                SCHOOL, LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 10)));
    }

    @Test
    void weekendOnlyRangeIsZero() {
        when(eventRepository.findInRange(SCHOOL, LocalDate.of(2026, 4, 11), LocalDate.of(2026, 4, 12)))
                .thenReturn(List.of());
        LeaveDayCalculator calculator = new LeaveDayCalculator(eventRepository);
        assertEquals(BigDecimal.ZERO, calculator.countWorkingDays(
                SCHOOL, LocalDate.of(2026, 4, 11), LocalDate.of(2026, 4, 12)));
    }
}
