package com.schoolms.academics;

import com.schoolms.academics.dto.TimetableEntryDto;
import com.schoolms.academics.dto.TimetableEntryRequest;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableEntryRepository timetableRepository;
    private final SectionRepository sectionRepository;
    private final SchoolClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherProfileRepository teacherRepository;
    private final AcademicYearRepository academicYearRepository;

    @Transactional(readOnly = true)
    public List<TimetableEntryDto> list(UUID academicYearId, UUID sectionId, UUID teacherId, String room) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        academicYearRepository.findByIdAndSchoolId(academicYearId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_year", academicYearId));
        if (sectionId != null) {
            sectionRepository.findByIdAndSchoolId(sectionId, schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("section", sectionId));
        }
        Map<UUID, Section> sections = new HashMap<>();
        Map<UUID, SchoolClass> classes = new HashMap<>();
        Map<UUID, Subject> subjects = new HashMap<>();
        Map<UUID, TeacherProfile> teachers = new HashMap<>();
        for (Section section : sectionRepository.findBySchoolIdOrderByNameAsc(schoolId)) {
            sections.put(section.getId(), section);
        }
        for (SchoolClass schoolClass : classRepository.findBySchoolIdOrderBySortOrderAsc(schoolId)) {
            classes.put(schoolClass.getId(), schoolClass);
        }
        for (Subject subject : subjectRepository.findBySchoolIdOrderByNameAsc(schoolId)) {
            subjects.put(subject.getId(), subject);
        }
        for (TeacherProfile teacher : teacherRepository.findBySchoolIdOrderByFirstNameAsc(schoolId)) {
            teachers.put(teacher.getId(), teacher);
        }
        String roomFilter = blankToNull(room);
        return timetableRepository
                .findBySchoolIdAndAcademicYearIdOrderByDayOfWeekAscPeriodNumberAsc(schoolId, academicYearId)
                .stream()
                .filter(entry -> sectionId == null || sectionId.equals(entry.getSectionId()))
                .filter(entry -> teacherId == null || teacherId.equals(entry.getTeacherId()))
                .filter(entry -> roomFilter == null
                        || (entry.getRoom() != null && entry.getRoom().equalsIgnoreCase(roomFilter)))
                .map(entry -> toDto(entry, sections, classes, subjects, teachers))
                .toList();
    }

    @Transactional
    public TimetableEntryDto create(TimetableEntryRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        validate(request, schoolId);
        assertNoConflicts(request, schoolId, null);
        TimetableEntry entry = new TimetableEntry();
        apply(entry, request, schoolId);
        return toDto(timetableRepository.save(entry));
    }

    @Transactional
    public TimetableEntryDto update(UUID id, TimetableEntryRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        TimetableEntry entry = timetableRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("timetable", id));
        validate(request, schoolId);
        assertNoConflicts(request, schoolId, id);
        apply(entry, request, schoolId);
        return toDto(timetableRepository.save(entry));
    }

    @Transactional
    public void delete(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        TimetableEntry entry = timetableRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("timetable", id));
        timetableRepository.delete(entry);
    }

    private void validate(TimetableEntryRequest request, UUID schoolId) {
        if (request.dayOfWeek() < 1 || request.dayOfWeek() > 6) {
            throw new BusinessException("validation.invalid");
        }
        if (request.periodNumber() < 1 || request.periodNumber() > 8) {
            throw new BusinessException("validation.invalid");
        }
        sectionRepository.findByIdAndSchoolId(request.sectionId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("section", request.sectionId()));
        academicYearRepository.findByIdAndSchoolId(request.academicYearId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_year", request.academicYearId()));
        if (request.subjectId() != null) {
            subjectRepository.findByIdAndSchoolId(request.subjectId(), schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("subject", request.subjectId()));
        }
        if (request.teacherId() != null) {
            teacherRepository.findByIdAndSchoolId(request.teacherId(), schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("teacher", request.teacherId()));
        }
        if (!request.startTime().isBefore(request.endTime())) {
            throw new BusinessException("validation.invalid");
        }
    }

    private void assertNoConflicts(TimetableEntryRequest request, UUID schoolId, UUID excludeId) {
        boolean slotTaken = excludeId == null
                ? timetableRepository.existsBySectionIdAndAcademicYearIdAndDayOfWeekAndPeriodNumber(
                        request.sectionId(), request.academicYearId(), request.dayOfWeek(), request.periodNumber())
                : timetableRepository.existsBySectionIdAndAcademicYearIdAndDayOfWeekAndPeriodNumberAndIdNot(
                        request.sectionId(), request.academicYearId(), request.dayOfWeek(),
                        request.periodNumber(), excludeId);
        if (slotTaken) {
            throw new BusinessException("timetable.conflict");
        }
        if (request.teacherId() != null) {
            boolean teacherBusy = excludeId == null
                    ? timetableRepository.existsByTeacherIdAndAcademicYearIdAndDayOfWeekAndPeriodNumber(
                            request.teacherId(), request.academicYearId(), request.dayOfWeek(), request.periodNumber())
                    : timetableRepository.existsByTeacherIdAndAcademicYearIdAndDayOfWeekAndPeriodNumberAndIdNot(
                            request.teacherId(), request.academicYearId(), request.dayOfWeek(),
                            request.periodNumber(), excludeId);
            if (teacherBusy) {
                throw new BusinessException("timetable.teacher_conflict");
            }
        }
        String room = blankToNull(request.room());
        if (room != null) {
            boolean roomBusy = excludeId == null
                    ? timetableRepository.existsBySchoolIdAndAcademicYearIdAndDayOfWeekAndPeriodNumberAndRoomIgnoreCase(
                            schoolId, request.academicYearId(), request.dayOfWeek(), request.periodNumber(), room)
                    : timetableRepository.existsBySchoolIdAndAcademicYearIdAndDayOfWeekAndPeriodNumberAndRoomIgnoreCaseAndIdNot(
                            schoolId, request.academicYearId(), request.dayOfWeek(),
                            request.periodNumber(), room, excludeId);
            if (roomBusy) {
                throw new BusinessException("timetable.room_conflict");
            }
        }
    }

    private void apply(TimetableEntry entry, TimetableEntryRequest request, UUID schoolId) {
        entry.setSchoolId(schoolId);
        entry.setSectionId(request.sectionId());
        entry.setAcademicYearId(request.academicYearId());
        entry.setDayOfWeek(request.dayOfWeek());
        entry.setPeriodNumber(request.periodNumber());
        entry.setStartTime(request.startTime());
        entry.setEndTime(request.endTime());
        entry.setSubjectId(request.subjectId());
        entry.setTeacherId(request.teacherId());
        entry.setRoom(blankToNull(request.room()));
    }

    private TimetableEntryDto toDto(TimetableEntry entry) {
        UUID schoolId = entry.getSchoolId();
        Map<UUID, Section> sections = new HashMap<>();
        sectionRepository.findByIdAndSchoolId(entry.getSectionId(), schoolId)
                .ifPresent(section -> sections.put(section.getId(), section));
        Map<UUID, SchoolClass> classes = new HashMap<>();
        Map<UUID, Subject> subjects = new HashMap<>();
        Map<UUID, TeacherProfile> teachers = new HashMap<>();
        Section section = sections.get(entry.getSectionId());
        if (section != null) {
            classRepository.findByIdAndSchoolId(section.getClassId(), schoolId)
                    .ifPresent(schoolClass -> classes.put(schoolClass.getId(), schoolClass));
        }
        if (entry.getSubjectId() != null) {
            subjectRepository.findByIdAndSchoolId(entry.getSubjectId(), schoolId)
                    .ifPresent(subject -> subjects.put(subject.getId(), subject));
        }
        if (entry.getTeacherId() != null) {
            teacherRepository.findByIdAndSchoolId(entry.getTeacherId(), schoolId)
                    .ifPresent(teacher -> teachers.put(teacher.getId(), teacher));
        }
        return toDto(entry, sections, classes, subjects, teachers);
    }

    private TimetableEntryDto toDto(TimetableEntry entry,
                                    Map<UUID, Section> sections,
                                    Map<UUID, SchoolClass> classes,
                                    Map<UUID, Subject> subjects,
                                    Map<UUID, TeacherProfile> teachers) {
        Section section = sections.get(entry.getSectionId());
        SchoolClass schoolClass = section == null ? null : classes.get(section.getClassId());
        Subject subject = entry.getSubjectId() == null ? null : subjects.get(entry.getSubjectId());
        TeacherProfile teacher = entry.getTeacherId() == null ? null : teachers.get(entry.getTeacherId());
        return new TimetableEntryDto(
                entry.getId(),
                entry.getSectionId(),
                section == null ? null : section.getName(),
                schoolClass == null ? null : schoolClass.getName(),
                entry.getAcademicYearId(),
                entry.getDayOfWeek(),
                entry.getPeriodNumber(),
                entry.getStartTime(),
                entry.getEndTime(),
                entry.getSubjectId(),
                subject == null ? null : subject.getName(),
                entry.getTeacherId(),
                teacher == null ? null : teacher.getDisplayName(),
                entry.getRoom());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
