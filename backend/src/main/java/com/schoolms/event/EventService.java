package com.schoolms.event;

import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.event.dto.EventDto;
import com.schoolms.event.dto.EventRequest;
import com.schoolms.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<EventDto> listVisible(String from, String to, String type) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        return eventRepository.findBySchoolIdOrderByStartDateTimeAsc(schoolId).stream()
                .filter(event -> matchesRange(event, from, to))
                .filter(event -> type == null || type.isBlank() || event.getType().equalsIgnoreCase(type))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventDto> upcoming() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        Instant now = Instant.now();
        return eventRepository.findBySchoolIdOrderByStartDateTimeAsc(schoolId).stream()
                .filter(event -> event.getStartDateTime() != null && !event.getStartDateTime().isBefore(now))
                .limit(5)
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public EventDto create(EventRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        Event event = new Event();
        event.setSchoolId(schoolId);
        event.setCreatedBy(SecurityUtils.currentUserId());
        apply(event, request);
        return toDto(eventRepository.save(event));
    }

    @Transactional
    public EventDto update(UUID id, EventRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        Event event = eventRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("event", id));
        apply(event, request);
        return toDto(eventRepository.save(event));
    }

    @Transactional
    public void delete(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        Event event = eventRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("event", id));
        eventRepository.delete(event);
    }

    private void apply(Event event, EventRequest request) {
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setType(normaliseType(request.type()));
        event.setStartDateTime(request.startDateTime());
        event.setEndDateTime(request.endDateTime());
        event.setAllDay(request.allDay());
        event.setAudienceScope(request.audienceScope() == null || request.audienceScope().isBlank() ? "ALL" : request.audienceScope().trim().toUpperCase());
        event.setAudienceRefId(request.audienceRefId());
        event.setSource(request.source() == null || request.source().isBlank() ? "MANUAL" : request.source().trim().toUpperCase());
        event.setSyncStatus(request.syncStatus() == null || request.syncStatus().isBlank() ? "NA" : request.syncStatus().trim().toUpperCase());
        event.setExternalProvider(request.externalProvider());
        event.setExternalRefId(request.externalRefId());
    }

    private String normaliseType(String type) {
        if (type == null || type.isBlank()) {
            return "OTHER";
        }
        String value = type.trim().toUpperCase();
        if (value.equals("SPORTS") || value.equals("SPORTS/CULTURAL EVENT") || value.equals("SPORTS_CULTURAL_EVENT")) {
            return "SPORTS_CULTURAL_EVENT";
        }
        if (value.equals("PTM")) {
            return "PTM";
        }
        if (value.equals("NOTICE_LINKED_EVENT") || value.equals("NOTICE-LINKED EVENT")) {
            return "NOTICE_LINKED_EVENT";
        }
        return switch (value) {
            case "HOLIDAY", "EXAM", "PTM", "SPORTS_CULTURAL_EVENT", "NOTICE_LINKED_EVENT", "OTHER" -> value;
            default -> "OTHER";
        };
    }

    private boolean matchesRange(Event event, String from, String to) {
        if (from == null || from.isBlank()) {
            from = "";
        }
        if (to == null || to.isBlank()) {
            to = "";
        }
        if (from.isBlank() && to.isBlank()) {
            return true;
        }
        Instant start = event.getStartDateTime();
        if (start == null) {
            return false;
        }
        if (!from.isBlank()) {
            try {
                Instant fromInstant = Instant.parse(from);
                if (start.isBefore(fromInstant)) {
                    return false;
                }
            } catch (Exception e) {
                throw new BusinessException("validation.invalid_date");
            }
        }
        if (!to.isBlank()) {
            try {
                Instant toInstant = Instant.parse(to);
                if (start.isAfter(toInstant)) {
                    return false;
                }
            } catch (Exception e) {
                throw new BusinessException("validation.invalid_date");
            }
        }
        return true;
    }

    private EventDto toDto(Event event) {
        return new EventDto(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getType(),
                event.getStartDateTime(),
                event.getEndDateTime(),
                event.isAllDay(),
                event.getAudienceScope(),
                event.getAudienceRefId(),
                event.getSource(),
                event.getSyncStatus(),
                event.getExternalProvider(),
                event.getExternalRefId(),
                event.getCreatedBy()
        );
    }
}
