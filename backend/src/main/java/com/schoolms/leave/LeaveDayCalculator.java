package com.schoolms.leave;

import com.schoolms.common.enums.EventType;
import com.schoolms.event.SchoolEvent;
import com.schoolms.event.SchoolEventRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
public class LeaveDayCalculator {

    private final SchoolEventRepository eventRepository;

    public LeaveDayCalculator(SchoolEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public BigDecimal countWorkingDays(UUID schoolId, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            return BigDecimal.ZERO;
        }
        Set<LocalDate> holidays = holidayDates(schoolId, from, to);
        int days = 0;
        for (LocalDate cursor = from; !cursor.isAfter(to); cursor = cursor.plusDays(1)) {
            DayOfWeek dow = cursor.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                continue;
            }
            if (holidays.contains(cursor)) {
                continue;
            }
            days++;
        }
        return BigDecimal.valueOf(days);
    }

    private Set<LocalDate> holidayDates(UUID schoolId, LocalDate from, LocalDate to) {
        Set<LocalDate> dates = new HashSet<>();
        for (SchoolEvent event : eventRepository.findInRange(schoolId, from, to)) {
            if (event.getEventType() != EventType.HOLIDAY) {
                continue;
            }
            LocalDate start = event.getStartDate();
            LocalDate end = event.getEndDate() == null ? start : event.getEndDate();
            for (LocalDate cursor = start; !cursor.isAfter(end); cursor = cursor.plusDays(1)) {
                if (!cursor.isBefore(from) && !cursor.isAfter(to)) {
                    dates.add(cursor);
                }
            }
        }
        return dates;
    }
}
