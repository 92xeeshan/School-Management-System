package com.schoolms.common;

import com.schoolms.academics.StudentEnrollmentRepository;
import com.schoolms.academics.TeacherProfileRepository;
import com.schoolms.staff.NonTeachingStaffRepository;
import com.schoolms.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RecordIdGenerator {

    public static final String STUDENT_PREFIX = "ADM";
    public static final String TEACHER_PREFIX = "EMP";
    public static final String NON_TEACHING_PREFIX = "NTS";

    private final StudentRepository studentRepository;
    private final TeacherProfileRepository teacherRepository;
    private final NonTeachingStaffRepository nonTeachingStaffRepository;
    private final StudentEnrollmentRepository enrollmentRepository;

    public String nextAdmissionNo(UUID schoolId) {
        String head = head(STUDENT_PREFIX);
        return nextFromExisting(STUDENT_PREFIX, studentRepository.findAdmissionNosByPrefix(schoolId, head));
    }

    public String nextTeacherEmployeeNo(UUID schoolId) {
        String head = head(TEACHER_PREFIX);
        return nextFromExisting(TEACHER_PREFIX, teacherRepository.findEmployeeNosByPrefix(schoolId, head));
    }

    public String nextNonTeachingEmployeeNo(UUID schoolId) {
        String head = head(NON_TEACHING_PREFIX);
        return nextFromExisting(NON_TEACHING_PREFIX,
                nonTeachingStaffRepository.findEmployeeNosByPrefix(schoolId, head));
    }

    public int nextRollNumber(UUID sectionId, UUID academicYearId) {
        Integer max = enrollmentRepository.findMaxRollNumber(sectionId, academicYearId);
        return (max == null ? 0 : max) + 1;
    }

    public String nextFromExisting(String prefix, Collection<String> existing) {
        String head = head(prefix);
        int max = 0;
        List<String> values = existing == null ? List.of() : List.copyOf(existing);
        for (String value : values) {
            Integer seq = parseSequence(head, value);
            if (seq != null) {
                max = Math.max(max, seq);
            }
        }
        return head + String.format("%03d", max + 1);
    }

    static String head(String prefix) {
        return prefix + Year.now().getValue();
    }

    static Integer parseSequence(String head, String value) {
        if (value == null || !value.startsWith(head)) {
            return null;
        }
        String suffix = value.substring(head.length());
        if (suffix.isEmpty() || !suffix.chars().allMatch(Character::isDigit)) {
            return null;
        }
        try {
            return Integer.parseInt(suffix);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
