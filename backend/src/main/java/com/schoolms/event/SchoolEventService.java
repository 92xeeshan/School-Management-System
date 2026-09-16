package com.schoolms.event;

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
import com.schoolms.common.enums.EventType;
import com.schoolms.common.enums.EventVisibility;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.event.dto.SchoolEventDto;
import com.schoolms.event.dto.SchoolEventRequest;
import com.schoolms.security.SecurityUtils;
import com.schoolms.student.GuardianRepository;
import com.schoolms.student.Student;
import com.schoolms.student.StudentGuardian;
import com.schoolms.student.StudentGuardianRepository;
import com.schoolms.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchoolEventService {

    private static final Set<String> STAFF_ROLES =
            Set.of("SUPER_ADMIN", "ADMIN", "TEACHER");

    private final SchoolEventRepository eventRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final TeacherProfileRepository teacherRepository;
    private final TeacherSectionRepository teacherSectionRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;

    @Transactional(readOnly = true)
    public List<SchoolEventDto> list(LocalDate from, LocalDate to) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        LocalDate startDate = from == null ? LocalDate.now().minusDays(30) : from;
        LocalDate endDate = to == null ? LocalDate.now().plusDays(60) : to;
        if (endDate.isBefore(startDate)) {
            throw new BusinessException("validation.invalid");
        }
        UserScope scope = userScope(schoolId);
        return eventRepository.findInRange(schoolId, startDate, endDate).stream()
                .filter(e -> visible(e, scope))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchoolEventDto> upcoming(int days) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        int horizon = days <= 0 ? 7 : Math.min(days, 120);
        UserScope scope = userScope(schoolId);
        LocalDate today = LocalDate.now();
        return eventRepository.findInRange(schoolId, today, today.plusDays(horizon)).stream()
                .filter(e -> visible(e, scope))
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public SchoolEventDto create(SchoolEventRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        SchoolEvent event = new SchoolEvent();
        event.setSchoolId(schoolId);
        event.setCreatedBy(SecurityUtils.currentUserId());
        apply(event, request);
        return toDto(eventRepository.save(event));
    }

    @Transactional
    public SchoolEventDto update(UUID id, SchoolEventRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        SchoolEvent event = eventRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("event", id));
        apply(event, request);
        return toDto(eventRepository.save(event));
    }

    @Transactional
    public void delete(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        SchoolEvent event = eventRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("event", id));
        eventRepository.delete(event);
    }

    private void apply(SchoolEvent event, SchoolEventRequest request) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BusinessException("validation.invalid");
        }
        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setEventType(parseType(request.eventType()));
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate());
        event.setAllDay(request.allDay() == null || request.allDay());
        event.setStartTime(event.isAllDay() ? null : request.startTime());
        event.setEndTime(event.isAllDay() ? null : request.endTime());
        event.setLocation(request.location());
        event.setVisibilityScope(parseVisibility(request.visibilityScope()));
        event.setClassId(request.classId());
        event.setSectionId(request.sectionId());
    }

    private boolean visible(SchoolEvent event, UserScope scope) {
        return switch (event.getVisibilityScope()) {
            case SCHOOL_WIDE -> true;
            case STAFF -> scope.staff();
            case CLASS_WIDE -> scope.staff()
                    || (event.getSectionId() != null && scope.sectionIds().contains(event.getSectionId()))
                    || (event.getClassId() != null && scope.classIds().contains(event.getClassId()));
        };
    }

    private UserScope userScope(UUID schoolId) {
        UUID userId = SecurityUtils.currentUserId();
        boolean staff = SecurityUtils.currentPrincipal().permissions().contains("EVENT_MANAGE")
                || SecurityUtils.currentPrincipal().roles().stream().anyMatch(STAFF_ROLES::contains);

        Set<UUID> sectionIds = new HashSet<>();
        Set<UUID> classIds = new HashSet<>();

        teacherRepository.findBySchoolIdAndUserId(schoolId, userId).ifPresent(teacher -> {
            for (TeacherSection ts : teacherSectionRepository.findByTeacherIdAndSchoolId(teacher.getId(), schoolId)) {
                sectionIds.add(ts.getSectionId());
                sectionRepository.findByIdAndSchoolId(ts.getSectionId(), schoolId)
                        .map(Section::getClassId).ifPresent(classIds::add);
            }
        });

        studentRepository.findBySchoolIdAndUserId(schoolId, userId).ifPresent(student ->
                collectStudentScope(student, schoolId, sectionIds, classIds));

        guardianRepository.findBySchoolIdAndUserId(schoolId, userId).ifPresent(guardian ->
                studentGuardianRepository.findWithStudents(schoolId, guardian.getId()).stream()
                        .map(StudentGuardian::getStudent)
                        .forEach(student -> collectStudentScope(student, schoolId, sectionIds, classIds)));

        return new UserScope(staff, sectionIds, classIds);
    }

    private void collectStudentScope(Student student, UUID schoolId,
                                     Set<UUID> sectionIds, Set<UUID> classIds) {
        UUID yearId = academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId)
                .map(AcademicYear::getId).orElse(null);
        if (yearId == null) {
            return;
        }
        enrollmentRepository.findByStudentIdAndAcademicYearId(student.getId(), yearId)
                .ifPresent((StudentEnrollment enrollment) -> {
                    sectionIds.add(enrollment.getSectionId());
                    sectionRepository.findByIdAndSchoolId(enrollment.getSectionId(), schoolId)
                            .map(Section::getClassId).ifPresent(classIds::add);
                });
    }

    private SchoolEventDto toDto(SchoolEvent event) {
        UUID schoolId = event.getSchoolId();
        String sectionName = event.getSectionId() == null ? null
                : sectionRepository.findByIdAndSchoolId(event.getSectionId(), schoolId)
                        .map(Section::getName).orElse(null);
        String className = event.getClassId() == null ? null
                : classRepository.findByIdAndSchoolId(event.getClassId(), schoolId)
                        .map(SchoolClass::getName).orElse(null);
        return new SchoolEventDto(event.getId(), event.getTitle(), event.getDescription(),
                event.getEventType().name(), event.getStartDate(), event.getEndDate(),
                event.isAllDay(), event.getStartTime(), event.getEndTime(), event.getLocation(),
                event.getVisibilityScope().name(), event.getClassId(), className,
                event.getSectionId(), sectionName);
    }

    private EventType parseType(String value) {
        if (value == null || value.isBlank()) {
            return EventType.EVENT;
        }
        try {
            return EventType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("validation.invalid");
        }
    }

    private EventVisibility parseVisibility(String value) {
        if (value == null || value.isBlank()) {
            return EventVisibility.SCHOOL_WIDE;
        }
        try {
            return EventVisibility.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("validation.invalid");
        }
    }

    private record UserScope(boolean staff, Set<UUID> sectionIds, Set<UUID> classIds) {
    }
}
