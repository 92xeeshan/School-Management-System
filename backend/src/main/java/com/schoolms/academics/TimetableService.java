package com.schoolms.academics;

import com.schoolms.academics.dto.TimetableEntryDto;
import com.schoolms.academics.dto.TimetableEntryRequest;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    public List<TimetableEntryDto> list(UUID sectionId, UUID academicYearId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        sectionRepository.findByIdAndSchoolId(sectionId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("section", sectionId));
        return timetableRepository
                .findBySectionIdAndAcademicYearIdOrderByDayOfWeekAscPeriodNumberAsc(sectionId, academicYearId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public TimetableEntryDto create(TimetableEntryRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        validate(request, schoolId);
        if (timetableRepository.existsBySectionIdAndAcademicYearIdAndDayOfWeekAndPeriodNumber(
                request.sectionId(), request.academicYearId(), request.dayOfWeek(), request.periodNumber())) {
            throw new BusinessException("timetable.conflict");
        }
        TimetableEntry entry = new TimetableEntry();
        apply(entry, request, schoolId);
        return toDto(timetableRepository.save(entry));
    }

    @Transactional
    public void delete(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        TimetableEntry entry = timetableRepository.findById(id)
                .filter(e -> e.getSchoolId().equals(schoolId))
                .orElseThrow(() -> ResourceNotFoundException.of("timetable", id));
        timetableRepository.delete(entry);
    }

    private void validate(TimetableEntryRequest request, UUID schoolId) {
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
        entry.setRoom(request.room());
    }

    private TimetableEntryDto toDto(TimetableEntry entry) {
        UUID schoolId = entry.getSchoolId();
        String sectionName = sectionRepository.findByIdAndSchoolId(entry.getSectionId(), schoolId)
                .map(Section::getName).orElse(null);
        String className = sectionRepository.findByIdAndSchoolId(entry.getSectionId(), schoolId)
                .flatMap(section -> classRepository.findByIdAndSchoolId(section.getClassId(), schoolId))
                .map(SchoolClass::getName).orElse(null);
        String subjectName = entry.getSubjectId() == null ? null
                : subjectRepository.findByIdAndSchoolId(entry.getSubjectId(), schoolId)
                        .map(Subject::getName).orElse(null);
        String teacherName = entry.getTeacherId() == null ? null
                : teacherRepository.findByIdAndSchoolId(entry.getTeacherId(), schoolId)
                        .map(TeacherProfile::getDisplayName).orElse(null);
        return new TimetableEntryDto(
                entry.getId(), entry.getSectionId(), sectionName, className,
                entry.getAcademicYearId(), entry.getDayOfWeek(), entry.getPeriodNumber(),
                entry.getStartTime(), entry.getEndTime(), entry.getSubjectId(), subjectName,
                entry.getTeacherId(), teacherName, entry.getRoom());
    }
}
