package com.schoolms.exam;

import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
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
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.exam.dto.ExamScheduleDto;
import com.schoolms.exam.dto.ExamScheduleOptionsDto;
import com.schoolms.exam.dto.ExamScheduleRequest;
import com.schoolms.exam.dto.ScheduleConflictDto;
import com.schoolms.exam.dto.ScheduleValidateResponse;
import com.schoolms.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamScheduleService {

    static final List<String> EXAM_TERMS = List.of("QUIZ", "UNIT", "MIDTERM", "TERM", "FINAL", "CONTINUOUS");
    private static final Set<String> STATUSES = Set.of("DRAFT", "PUBLISHED");

    private final ExamScheduleRepository scheduleRepository;
    private final ExamScheduleInvigilatorRepository invigilatorRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final TeacherProfileRepository teacherRepository;

    @Transactional(readOnly = true)
    public ExamScheduleOptionsDto options() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        List<ExamScheduleOptionsDto.YearOption> years = academicYearRepository
                .findBySchoolIdOrderByStartDateDesc(schoolId).stream()
                .map(year -> new ExamScheduleOptionsDto.YearOption(year.getId(), year.getName(), year.isCurrent()))
                .toList();

        Map<UUID, List<Section>> sectionsByClass = sectionRepository.findBySchoolIdOrderByNameAsc(schoolId)
                .stream()
                .collect(Collectors.groupingBy(Section::getClassId));
        List<ExamScheduleOptionsDto.ClassOption> classes = classRepository
                .findBySchoolIdOrderBySortOrderAsc(schoolId).stream()
                .map(klass -> new ExamScheduleOptionsDto.ClassOption(
                        klass.getId(),
                        klass.getName(),
                        sectionsByClass.getOrDefault(klass.getId(), List.of()).stream()
                                .map(section -> new ExamScheduleOptionsDto.SectionOption(
                                        section.getId(), section.getName(), section.getRoom()))
                                .toList()))
                .toList();

        List<ExamScheduleOptionsDto.SubjectOption> subjects = subjectRepository
                .findBySchoolIdOrderByNameAsc(schoolId).stream()
                .map(subject -> new ExamScheduleOptionsDto.SubjectOption(subject.getId(), subject.getName()))
                .toList();

        List<ExamScheduleOptionsDto.TeacherOption> teachers = teacherRepository
                .findBySchoolIdOrderByFirstNameAsc(schoolId).stream()
                .filter(teacher -> "ACTIVE".equals(teacher.getStatus()))
                .map(teacher -> new ExamScheduleOptionsDto.TeacherOption(teacher.getId(), teacher.getDisplayName()))
                .toList();

        LinkedHashSet<String> rooms = new LinkedHashSet<>();
        for (Section section : sectionRepository.findBySchoolIdOrderByNameAsc(schoolId)) {
            if (blankToNull(section.getRoom()) != null) {
                rooms.add(section.getRoom());
            }
        }
        UUID currentYearId = years.stream()
                .filter(ExamScheduleOptionsDto.YearOption::current)
                .map(ExamScheduleOptionsDto.YearOption::id)
                .findFirst()
                .orElse(years.isEmpty() ? null : years.get(0).id());
        if (currentYearId != null) {
            scheduleRepository.findBySchoolIdAndAcademicYearIdOrderByExamDateAscStartTimeAsc(schoolId, currentYearId)
                    .forEach(schedule -> {
                        if (blankToNull(schedule.getRoom()) != null) {
                            rooms.add(schedule.getRoom());
                        }
                    });
        }

        return new ExamScheduleOptionsDto(years, classes, subjects, teachers, List.copyOf(rooms), EXAM_TERMS);
    }

    @Transactional(readOnly = true)
    public List<ExamScheduleDto> list(UUID academicYearId, UUID classId, UUID sectionId, String room) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        academicYearRepository.findByIdAndSchoolId(academicYearId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_year", academicYearId));
        String roomFilter = blankToNull(room);
        List<ExamSchedule> schedules = scheduleRepository
                .findBySchoolIdAndAcademicYearIdOrderByExamDateAscStartTimeAsc(schoolId, academicYearId)
                .stream()
                .filter(schedule -> classId == null || classId.equals(schedule.getClassId()))
                .filter(schedule -> sectionId == null || sectionId.equals(schedule.getSectionId()))
                .filter(schedule -> roomFilter == null
                        || (schedule.getRoom() != null && schedule.getRoom().equalsIgnoreCase(roomFilter)))
                .toList();
        Lookup lookup = lookup(schoolId, schedules.stream().map(ExamSchedule::getId).toList());
        return schedules.stream().map(schedule -> toDto(schedule, lookup)).toList();
    }

    @Transactional(readOnly = true)
    public ScheduleValidateResponse validate(ExamScheduleRequest request, UUID excludeId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        validateRequest(request, schoolId);
        List<ScheduleConflictDto> conflicts = findConflicts(request, schoolId, excludeId);
        return new ScheduleValidateResponse(conflicts.isEmpty(), conflicts);
    }

    @Transactional
    public ExamScheduleDto create(ExamScheduleRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        validateRequest(request, schoolId);
        assertNoConflicts(request, schoolId, null);
        assertUniqueSubject(request, schoolId, null);
        ExamSchedule schedule = new ExamSchedule();
        apply(schedule, request, schoolId);
        ExamSchedule saved = scheduleRepository.save(schedule);
        replaceInvigilators(saved, request.invigilatorIds(), schoolId);
        return toDto(saved, lookup(schoolId, List.of(saved.getId())));
    }

    @Transactional
    public ExamScheduleDto update(UUID id, ExamScheduleRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        ExamSchedule schedule = scheduleRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("exam_schedule", id));
        validateRequest(request, schoolId);
        assertNoConflicts(request, schoolId, id);
        assertUniqueSubject(request, schoolId, id);
        apply(schedule, request, schoolId);
        ExamSchedule saved = scheduleRepository.save(schedule);
        replaceInvigilators(saved, request.invigilatorIds(), schoolId);
        return toDto(saved, lookup(schoolId, List.of(saved.getId())));
    }

    @Transactional
    public ExamScheduleDto updateStatus(UUID id, String status) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        ExamSchedule schedule = scheduleRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("exam_schedule", id));
        String normalized = normalizeStatus(status);
        if ("PUBLISHED".equals(normalized)) {
            ExamScheduleRequest snapshot = toRequest(schedule);
            assertNoConflicts(snapshot, schoolId, id);
        }
        schedule.setStatus(normalized);
        return toDto(scheduleRepository.save(schedule), lookup(schoolId, List.of(schedule.getId())));
    }

    @Transactional
    public void delete(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        ExamSchedule schedule = scheduleRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("exam_schedule", id));
        invigilatorRepository.deleteByExamScheduleId(schedule.getId());
        scheduleRepository.delete(schedule);
    }

    private void validateRequest(ExamScheduleRequest request, UUID schoolId) {
        academicYearRepository.findByIdAndSchoolId(request.academicYearId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_year", request.academicYearId()));
        classRepository.findByIdAndSchoolId(request.classId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("class", request.classId()));
        Section section = sectionRepository.findByIdAndSchoolId(request.sectionId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("section", request.sectionId()));
        if (!request.classId().equals(section.getClassId())) {
            throw new BusinessException("exam_schedule.section_mismatch");
        }
        subjectRepository.findByIdAndSchoolId(request.subjectId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("subject", request.subjectId()));
        boolean mapped = classSubjectRepository.findByClassIdAndSchoolId(request.classId(), schoolId).stream()
                .anyMatch(link -> request.subjectId().equals(link.getSubjectId()));
        if (!mapped) {
            throw new BusinessException("marks.subject_not_in_class");
        }
        if (!EXAM_TERMS.contains(request.examTerm())) {
            throw new BusinessException("exam_schedule.invalid_term");
        }
        if (!request.startTime().isBefore(request.endTime())) {
            throw new BusinessException("exam_schedule.invalid_time");
        }
        if (request.maxMarks() == null || request.maxMarks().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("exam_schedule.invalid_marks");
        }
        if (request.passMarks() == null
                || request.passMarks().compareTo(BigDecimal.ZERO) < 0
                || request.passMarks().compareTo(request.maxMarks()) > 0) {
            throw new BusinessException("exam_schedule.invalid_marks");
        }
        for (UUID teacherId : uniqueIds(request.invigilatorIds())) {
            teacherRepository.findByIdAndSchoolId(teacherId, schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("teacher", teacherId));
        }
        normalizeStatus(request.status());
    }

    private void assertUniqueSubject(ExamScheduleRequest request, UUID schoolId, UUID excludeId) {
        boolean exists = excludeId == null
                ? scheduleRepository.existsBySchoolIdAndAcademicYearIdAndSectionIdAndSubjectIdAndExamTerm(
                        schoolId, request.academicYearId(), request.sectionId(), request.subjectId(), request.examTerm())
                : scheduleRepository.existsBySchoolIdAndAcademicYearIdAndSectionIdAndSubjectIdAndExamTermAndIdNot(
                        schoolId, request.academicYearId(), request.sectionId(), request.subjectId(),
                        request.examTerm(), excludeId);
        if (exists) {
            throw new BusinessException("exam_schedule.duplicate");
        }
    }

    private void assertNoConflicts(ExamScheduleRequest request, UUID schoolId, UUID excludeId) {
        List<ScheduleConflictDto> conflicts = findConflicts(request, schoolId, excludeId);
        if (!conflicts.isEmpty()) {
            throw new BusinessException(conflicts.get(0).code());
        }
    }

    List<ScheduleConflictDto> findConflicts(ExamScheduleRequest request, UUID schoolId, UUID excludeId) {
        List<ExamSchedule> sameDay = scheduleRepository.findBySchoolIdAndExamDate(schoolId, request.examDate());
        Map<UUID, List<UUID>> invigilatorsBySchedule = invigilatorsBySchedule(
                sameDay.stream().map(ExamSchedule::getId).toList());
        Set<UUID> requestedInvigilators = uniqueIds(request.invigilatorIds());
        String room = blankToNull(request.room());
        List<ScheduleConflictDto> conflicts = new ArrayList<>();
        for (ExamSchedule existing : sameDay) {
            if (excludeId != null && excludeId.equals(existing.getId())) {
                continue;
            }
            if (!overlaps(request.startTime(), request.endTime(), existing.getStartTime(), existing.getEndTime())) {
                continue;
            }
            if (request.sectionId().equals(existing.getSectionId())) {
                conflicts.add(new ScheduleConflictDto("sectionId", "exam_schedule.section_overlap", existing.getId()));
            }
            if (room != null && existing.getRoom() != null && room.equalsIgnoreCase(existing.getRoom())) {
                conflicts.add(new ScheduleConflictDto("room", "exam_schedule.room_overlap", existing.getId()));
            }
            for (UUID teacherId : invigilatorsBySchedule.getOrDefault(existing.getId(), List.of())) {
                if (requestedInvigilators.contains(teacherId)) {
                    conflicts.add(new ScheduleConflictDto(
                            "invigilatorIds", "exam_schedule.invigilator_overlap", existing.getId()));
                    break;
                }
            }
        }
        return conflicts;
    }

    private Map<UUID, List<UUID>> invigilatorsBySchedule(List<UUID> scheduleIds) {
        if (scheduleIds.isEmpty()) {
            return Map.of();
        }
        return invigilatorRepository.findByExamScheduleIdIn(scheduleIds).stream()
                .collect(Collectors.groupingBy(
                        ExamScheduleInvigilator::getExamScheduleId,
                        Collectors.mapping(ExamScheduleInvigilator::getTeacherId, Collectors.toList())));
    }

    private void apply(ExamSchedule schedule, ExamScheduleRequest request, UUID schoolId) {
        schedule.setSchoolId(schoolId);
        schedule.setAcademicYearId(request.academicYearId());
        schedule.setClassId(request.classId());
        schedule.setSectionId(request.sectionId());
        schedule.setSubjectId(request.subjectId());
        schedule.setExamTerm(request.examTerm());
        schedule.setExamDate(request.examDate());
        schedule.setStartTime(request.startTime());
        schedule.setEndTime(request.endTime());
        schedule.setRoom(blankToNull(request.room()));
        schedule.setMaxMarks(request.maxMarks());
        schedule.setPassMarks(request.passMarks());
        schedule.setStatus(normalizeStatus(request.status()));
    }

    private void replaceInvigilators(ExamSchedule schedule, List<UUID> invigilatorIds, UUID schoolId) {
        invigilatorRepository.deleteByExamScheduleId(schedule.getId());
        for (UUID teacherId : uniqueIds(invigilatorIds)) {
            ExamScheduleInvigilator link = new ExamScheduleInvigilator();
            link.setSchoolId(schoolId);
            link.setExamScheduleId(schedule.getId());
            link.setTeacherId(teacherId);
            invigilatorRepository.save(link);
        }
    }

    private ExamScheduleDto toDto(ExamSchedule schedule, Lookup lookup) {
        SchoolClass klass = lookup.classes.get(schedule.getClassId());
        Section section = lookup.sections.get(schedule.getSectionId());
        Subject subject = lookup.subjects.get(schedule.getSubjectId());
        AcademicYear year = lookup.years.get(schedule.getAcademicYearId());
        List<ExamScheduleDto.InvigilatorRef> invigilators = lookup.invigilators
                .getOrDefault(schedule.getId(), List.of()).stream()
                .map(teacherId -> {
                    TeacherProfile teacher = lookup.teachers.get(teacherId);
                    return new ExamScheduleDto.InvigilatorRef(
                            teacherId, teacher == null ? null : teacher.getDisplayName());
                })
                .toList();
        return new ExamScheduleDto(
                schedule.getId(),
                schedule.getAcademicYearId(),
                year == null ? null : year.getName(),
                schedule.getClassId(),
                klass == null ? null : klass.getName(),
                schedule.getSectionId(),
                section == null ? null : section.getName(),
                schedule.getSubjectId(),
                subject == null ? null : subject.getName(),
                schedule.getExamTerm(),
                schedule.getExamDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getRoom(),
                schedule.getMaxMarks(),
                schedule.getPassMarks(),
                schedule.getStatus(),
                invigilators);
    }

    private Lookup lookup(UUID schoolId, List<UUID> scheduleIds) {
        Map<UUID, AcademicYear> years = academicYearRepository.findBySchoolIdOrderByStartDateDesc(schoolId)
                .stream().collect(Collectors.toMap(AcademicYear::getId, year -> year));
        Map<UUID, SchoolClass> classes = classRepository.findBySchoolIdOrderBySortOrderAsc(schoolId)
                .stream().collect(Collectors.toMap(SchoolClass::getId, klass -> klass));
        Map<UUID, Section> sections = sectionRepository.findBySchoolIdOrderByNameAsc(schoolId)
                .stream().collect(Collectors.toMap(Section::getId, section -> section));
        Map<UUID, Subject> subjects = subjectRepository.findBySchoolIdOrderByNameAsc(schoolId)
                .stream().collect(Collectors.toMap(Subject::getId, subject -> subject));
        Map<UUID, TeacherProfile> teachers = teacherRepository.findBySchoolIdOrderByFirstNameAsc(schoolId)
                .stream().collect(Collectors.toMap(TeacherProfile::getId, teacher -> teacher));
        return new Lookup(years, classes, sections, subjects, teachers, invigilatorsBySchedule(scheduleIds));
    }

    private ExamScheduleRequest toRequest(ExamSchedule schedule) {
        List<UUID> invigilatorIds = invigilatorRepository.findByExamScheduleId(schedule.getId()).stream()
                .map(ExamScheduleInvigilator::getTeacherId)
                .toList();
        return new ExamScheduleRequest(
                schedule.getAcademicYearId(),
                schedule.getClassId(),
                schedule.getSectionId(),
                schedule.getSubjectId(),
                schedule.getExamTerm(),
                schedule.getExamDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getRoom(),
                schedule.getMaxMarks(),
                schedule.getPassMarks(),
                invigilatorIds,
                schedule.getStatus());
    }

    private static boolean overlaps(LocalTime startA, LocalTime endA, LocalTime startB, LocalTime endB) {
        return startA.isBefore(endB) && startB.isBefore(endA);
    }

    private static String normalizeStatus(String status) {
        String value = status == null || status.isBlank() ? "DRAFT" : status.trim().toUpperCase();
        if (!STATUSES.contains(value)) {
            throw new BusinessException("exam_schedule.invalid_status");
        }
        return value;
    }

    private static Set<UUID> uniqueIds(List<UUID> ids) {
        if (ids == null) {
            return Set.of();
        }
        return ids.stream().filter(id -> id != null).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record Lookup(
            Map<UUID, AcademicYear> years,
            Map<UUID, SchoolClass> classes,
            Map<UUID, Section> sections,
            Map<UUID, Subject> subjects,
            Map<UUID, TeacherProfile> teachers,
            Map<UUID, List<UUID>> invigilators
    ) {
        Lookup {
            years = years == null ? new HashMap<>() : years;
            classes = classes == null ? new HashMap<>() : classes;
            sections = sections == null ? new HashMap<>() : sections;
            subjects = subjects == null ? new HashMap<>() : subjects;
            teachers = teachers == null ? new HashMap<>() : teachers;
            invigilators = invigilators == null ? new HashMap<>() : invigilators;
        }
    }
}
