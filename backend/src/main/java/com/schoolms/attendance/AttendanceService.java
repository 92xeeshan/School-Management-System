package com.schoolms.attendance;

import com.schoolms.attendance.dto.AttendanceMarkRequest;
import com.schoolms.attendance.dto.AttendanceRecordDto;
import com.schoolms.attendance.dto.AttendanceSessionDto;
import com.schoolms.attendance.dto.AttendanceSummaryDto;
import com.schoolms.common.enums.AttendanceSessionStatus;
import com.schoolms.common.enums.AttendanceStatus;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.security.SecurityUtils;
import com.schoolms.student.Student;
import com.schoolms.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final StudentRepository studentRepository;
    private final com.schoolms.academics.SectionRepository sectionRepository;
    private final com.schoolms.academics.AcademicYearRepository academicYearRepository;

    @Transactional(readOnly = true)
    public List<AttendanceSessionDto> listSessions(UUID sectionId, UUID academicYearId,
                                                   LocalDate from, LocalDate to) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        sectionRepository.findByIdAndSchoolId(sectionId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("section", sectionId));
        return sessionRepository
                .findBySectionIdAndAcademicYearIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
                        sectionId, academicYearId, from, to).stream()
                .map(s -> new AttendanceSessionDto(s.getId(), s.getSectionId(), s.getAcademicYearId(),
                        s.getAttendanceDate(), s.getSubjectId(), s.getStatus().name(),
                        s.getMarkedBy(), s.getMarkedAt()))
                .toList();
    }

    @Transactional
    public void mark(AttendanceMarkRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UUID userId = SecurityUtils.currentUserId();
        sectionRepository.findByIdAndSchoolId(request.sectionId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("section", request.sectionId()));
        academicYearRepository.findByIdAndSchoolId(request.academicYearId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_year", request.academicYearId()));
        if (request.attendanceDate().isAfter(LocalDate.now())) {
            throw new BusinessException("attendance.invalid_date");
        }

        AttendanceSession session = ensureSession(request, schoolId, userId);

        Map<UUID, Student> students = studentRepository
                .findBySchoolIdAndIdIn(schoolId,
                        request.marks().stream().map(AttendanceMarkRequest.StudentAttendanceMark::studentId).toList())
                .stream().collect(Collectors.toMap(Student::getId, Function.identity()));

        int marked = 0;
        for (AttendanceMarkRequest.StudentAttendanceMark mark : request.marks()) {
            Student student = students.get(mark.studentId());
            if (student == null) {
                throw new ResourceNotFoundException("student.not_found", mark.studentId());
            }
            upsert(request, student.getId(), mark, userId);
            marked++;
        }

        if (marked > 0) {
            session.setStatus(marked >= students.size()
                    ? AttendanceSessionStatus.COMPLETE : AttendanceSessionStatus.PARTIAL);
            session.setMarkedBy(userId);
            session.setMarkedAt(Instant.now());
            sessionRepository.save(session);
        }
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordDto> getSectionRecords(UUID sectionId, UUID academicYearId,
                                                       LocalDate date, UUID subjectId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        sectionRepository.findByIdAndSchoolId(sectionId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("section", sectionId));

        List<Student> students = studentRepository.findBySectionAndYear(schoolId, sectionId, academicYearId);
        Map<UUID, Attendance> existing = attendanceRepository
                .findBySectionIdAndAttendanceDateOrderByStudentIdAsc(sectionId, date).stream()
                .collect(Collectors.toMap(Attendance::getStudentId, Function.identity()));

        return students.stream().map(student -> {
            Attendance a = existing.get(student.getId());
            return new AttendanceRecordDto(
                    a == null ? null : a.getId(),
                    student.getId(),
                    student.getDisplayName(),
                    student.getAdmissionNo(),
                    sectionId,
                    date,
                    a == null ? null : a.getStatus().name(),
                    a == null ? null : a.getRemark());
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordDto> getStudentRecords(UUID studentId, LocalDate from, LocalDate to) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("student", studentId));
        return attendanceRepository
                .findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(studentId, from, to)
                .stream().map(a -> new AttendanceRecordDto(a.getId(), studentId, student.getDisplayName(),
                        student.getAdmissionNo(), a.getSectionId(), a.getAttendanceDate(),
                        a.getStatus().name(), a.getRemark()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryDto getStudentSummary(UUID studentId, LocalDate from, LocalDate to) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("student", studentId));
        List<Attendance> records = attendanceRepository
                .findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(studentId, from, to);
        long present = records.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        long absent = records.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
        long late = records.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
        long leave = records.stream().filter(a -> a.getStatus() == AttendanceStatus.LEAVE).count();
        long total = records.size();
        double percentage = total == 0 ? 0.0 : (double) (present + late) * 100.0 / total;
        return new AttendanceSummaryDto(studentId, student.getDisplayName(),
                present, absent, late, leave, total,
                Math.round(percentage * 100.0) / 100.0);
    }

    private AttendanceSession ensureSession(AttendanceMarkRequest request, UUID schoolId, UUID userId) {
        return sessionRepository
                .findBySectionIdAndAcademicYearIdAndAttendanceDateAndSubjectId(
                        request.sectionId(), request.academicYearId(), request.attendanceDate(), request.subjectId())
                .orElseGet(() -> {
                    AttendanceSession session = new AttendanceSession();
                    session.setSchoolId(schoolId);
                    session.setSectionId(request.sectionId());
                    session.setAcademicYearId(request.academicYearId());
                    session.setAttendanceDate(request.attendanceDate());
                    session.setSubjectId(request.subjectId());
                    session.setStatus(AttendanceSessionStatus.PENDING);
                    session.setMarkedBy(userId);
                    session.setMarkedAt(Instant.now());
                    return sessionRepository.save(session);
                });
    }

    private void upsert(AttendanceMarkRequest request, UUID studentId,
                        AttendanceMarkRequest.StudentAttendanceMark mark, UUID userId) {
        Attendance record = attendanceRepository
                .findByStudentIdAndAttendanceDate(studentId, request.attendanceDate())
                .orElseGet(() -> {
                    Attendance a = new Attendance();
                    a.setSchoolId(SecurityUtils.currentSchoolId());
                    a.setStudentId(studentId);
                    a.setSectionId(request.sectionId());
                    a.setAttendanceDate(request.attendanceDate());
                    return a;
                });
        record.setStatus(parseStatus(mark.status()));
        record.setRemark(mark.remark());
        record.setMarkedBy(userId);
        attendanceRepository.save(record);
    }

    private AttendanceStatus parseStatus(String value) {
        try {
            return AttendanceStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("validation.invalid");
        }
    }
}
