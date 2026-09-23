package com.schoolms.event;

import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.SchoolClass;
import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.academics.Section;
import com.schoolms.academics.SectionRepository;
import com.schoolms.academics.StudentEnrollment;
import com.schoolms.academics.StudentEnrollmentRepository;
import com.schoolms.academics.TeacherProfileRepository;
import com.schoolms.academics.TeacherSection;
import com.schoolms.academics.TeacherSectionRepository;
import com.schoolms.common.enums.EventType;
import com.schoolms.common.enums.EventVisibility;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.event.dto.EventOptionsDto;
import com.schoolms.event.dto.SchoolEventDto;
import com.schoolms.event.dto.SchoolEventRequest;
import com.schoolms.school.School;
import com.schoolms.school.SchoolRepository;
import com.schoolms.security.SecurityUtils;
import com.schoolms.security.UserPrincipal;
import com.schoolms.student.GuardianRepository;
import com.schoolms.student.Student;
import com.schoolms.student.StudentGuardian;
import com.schoolms.student.StudentGuardianRepository;
import com.schoolms.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchoolEventService {

    private static final Set<String> STAFF_ROLES =
            Set.of("SUPER_ADMIN", "ADMIN", "TEACHER");
    private static final Set<String> AUDIENCE_ROLES =
            Set.of("SUPER_ADMIN", "ADMIN", "TEACHER", "STUDENT", "PARENT");
    private static final int UPCOMING_LIMIT = 5;

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
    private final SchoolRepository schoolRepository;
    private final HolidayPdfService holidayPdfService;

    @Transactional(readOnly = true)
    public List<SchoolEventDto> list(LocalDate from, LocalDate to, String type) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        LocalDate startDate = from == null ? LocalDate.now().minusDays(30) : from;
        LocalDate endDate = to == null ? LocalDate.now().plusDays(60) : to;
        if (endDate.isBefore(startDate)) {
            throw new BusinessException("validation.invalid");
        }
        EventType filter = parseOptionalType(type);
        UserScope scope = userScope(schoolId);
        return eventRepository.findInRange(schoolId, startDate, endDate).stream()
                .filter(e -> visible(e, scope))
                .filter(e -> filter == null || e.getEventType() == filter)
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchoolEventDto> upcoming(int days) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        int horizon = days <= 0 ? 30 : Math.min(days, 120);
        UserScope scope = userScope(schoolId);
        LocalDate today = LocalDate.now();
        return eventRepository.findInRange(schoolId, today, today.plusDays(horizon)).stream()
                .filter(e -> visible(e, scope))
                .map(this::toDto)
                .limit(UPCOMING_LIMIT)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventOptionsDto options() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        List<Section> sections = sectionRepository.findBySchoolIdOrderByNameAsc(schoolId);
        List<EventOptionsDto.ClassOption> classes = classRepository.findBySchoolIdOrderBySortOrderAsc(schoolId)
                .stream()
                .map(klass -> new EventOptionsDto.ClassOption(
                        klass.getId(),
                        klass.getName(),
                        sections.stream()
                                .filter(section -> klass.getId().equals(section.getClassId()))
                                .map(section -> new EventOptionsDto.SectionOption(section.getId(), section.getName()))
                                .toList()))
                .toList();
        return new EventOptionsDto(
                Arrays.stream(EventType.values()).map(Enum::name).toList(),
                Arrays.stream(EventVisibility.values()).map(Enum::name).toList(),
                List.of("TEACHER", "STUDENT", "PARENT"),
                classes);
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

    @Transactional(readOnly = true)
    public byte[] exportHolidays(Integer year) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        int targetYear = year == null ? LocalDate.now().getYear() : year;
        LocalDate from = LocalDate.of(targetYear, 1, 1);
        LocalDate to = LocalDate.of(targetYear, 12, 31);
        UserScope scope = userScope(schoolId);
        List<SchoolEventDto> holidays = eventRepository.findInRange(schoolId, from, to).stream()
                .filter(e -> e.getEventType() == EventType.HOLIDAY)
                .filter(e -> visible(e, scope))
                .map(this::toDto)
                .toList();
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new BusinessException("school.not_found"));
        return holidayPdfService.render(school, targetYear, holidays);
    }

    public String contentDisposition(String filename) {
        return "attachment; filename=\"" + filename + "\"";
    }

    public HttpHeaders downloadHeaders(String filename, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(filename));
        headers.add(HttpHeaders.CONTENT_TYPE, contentType);
        return headers;
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
        EventVisibility visibility = parseVisibility(request.visibilityScope());
        event.setVisibilityScope(visibility);
        event.setAudienceRole(null);
        event.setClassId(null);
        event.setSectionId(null);
        if (visibility == EventVisibility.CLASS_WIDE) {
            applyClassAudience(event, request);
        } else if (visibility == EventVisibility.ROLE) {
            event.setAudienceRole(parseAudienceRole(request.audienceRole()));
        }
    }

    private void applyClassAudience(SchoolEvent event, SchoolEventRequest request) {
        UUID schoolId = event.getSchoolId();
        if (request.classId() == null) {
            throw new BusinessException("event.class_required");
        }
        SchoolClass klass = classRepository.findByIdAndSchoolId(request.classId(), schoolId)
                .orElseThrow(() -> new BusinessException("class.not_found"));
        event.setClassId(klass.getId());
        if (request.sectionId() != null) {
            Section section = sectionRepository.findByIdAndSchoolId(request.sectionId(), schoolId)
                    .orElseThrow(() -> new BusinessException("section.not_found"));
            if (!klass.getId().equals(section.getClassId())) {
                throw new BusinessException("event.section_mismatch");
            }
            event.setSectionId(section.getId());
        }
    }

    private boolean visible(SchoolEvent event, UserScope scope) {
        return switch (event.getVisibilityScope()) {
            case SCHOOL_WIDE -> true;
            case STAFF -> scope.staff();
            case CLASS_WIDE -> scope.admin()
                    || (event.getSectionId() != null && scope.sectionIds().contains(event.getSectionId()))
                    || (event.getSectionId() == null && event.getClassId() != null
                    && scope.classIds().contains(event.getClassId()));
            case ROLE -> scope.admin()
                    || (event.getAudienceRole() != null && scope.roles().contains(event.getAudienceRole()));
        };
    }

    private UserScope userScope(UUID schoolId) {
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        UUID userId = principal.id();
        boolean admin = principal.permissions().contains("EVENT_MANAGE")
                || principal.roles().stream().anyMatch(role -> role.equals("SUPER_ADMIN") || role.equals("ADMIN"));
        boolean staff = admin || principal.roles().stream().anyMatch(STAFF_ROLES::contains);

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

        return new UserScope(admin, staff, Set.copyOf(principal.roles()), sectionIds, classIds);
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
                event.getVisibilityScope().name(), event.getAudienceRole(), event.getClassId(), className,
                event.getSectionId(), sectionName, event.getUpdatedAt());
    }

    private EventType parseType(String value) {
        EventType type = parseOptionalType(value);
        return type == null ? EventType.EVENT : type;
    }

    private EventType parseOptionalType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return EventType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("validation.invalid");
        }
    }

    private EventVisibility parseVisibility(String value) {
        if (value == null || value.isBlank()) {
            return EventVisibility.SCHOOL_WIDE;
        }
        try {
            return EventVisibility.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("validation.invalid");
        }
    }

    private String parseAudienceRole(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("event.role_required");
        }
        String role = value.trim().toUpperCase(Locale.ROOT);
        if (!AUDIENCE_ROLES.contains(role)) {
            throw new BusinessException("validation.invalid");
        }
        return role;
    }

    record UserScope(boolean admin, boolean staff, Set<String> roles, Set<UUID> sectionIds, Set<UUID> classIds) {
    }
}
