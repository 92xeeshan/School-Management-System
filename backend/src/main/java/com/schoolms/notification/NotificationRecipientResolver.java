package com.schoolms.notification;

import com.schoolms.academics.ClassSubject;
import com.schoolms.academics.ClassSubjectRepository;
import com.schoolms.academics.TeacherProfileRepository;
import com.schoolms.academics.TeacherSection;
import com.schoolms.academics.TeacherSectionRepository;
import com.schoolms.common.enums.UserStatus;
import com.schoolms.student.Guardian;
import com.schoolms.student.StudentGuardian;
import com.schoolms.student.StudentGuardianRepository;
import com.schoolms.student.StudentRepository;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationRecipientResolver {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final TeacherSectionRepository teacherSectionRepository;
    private final ClassSubjectRepository classSubjectRepository;

    public List<UUID> adminReviewers(UUID schoolId) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (User user : userRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId)) {
            if (user.getStatus() != UserStatus.ACTIVE) {
                continue;
            }
            List<String> roles = userRepository.findRoleCodesByUserId(user.getId());
            if (roles.contains("ADMIN") || roles.contains("SUPER_ADMIN")) {
                ids.add(user.getId());
                continue;
            }
            List<String> permissions = userRepository.findPermissionCodesByUserId(user.getId());
            if (permissions.contains("MARKSHEET_MANAGE")) {
                ids.add(user.getId());
            }
        }
        return new ArrayList<>(ids);
    }

    public List<UUID> studentAndParents(UUID schoolId, UUID studentId) {
        Set<UUID> ids = new LinkedHashSet<>();
        studentRepository.findByIdAndSchoolId(studentId, schoolId).ifPresent(student -> addIfPresent(ids, student.getUserId()));
        for (StudentGuardian link : studentGuardianRepository.findWithGuardians(schoolId, studentId)) {
            Guardian guardian = link.getGuardian();
            if (guardian != null) {
                addIfPresent(ids, guardian.getUserId());
            }
        }
        return new ArrayList<>(ids);
    }

    public List<UUID> studentAndParents(UUID schoolId, Collection<UUID> studentIds) {
        Set<UUID> ids = new LinkedHashSet<>();
        if (studentIds == null) {
            return List.of();
        }
        for (UUID studentId : studentIds) {
            ids.addAll(studentAndParents(schoolId, studentId));
        }
        return new ArrayList<>(ids);
    }

    public List<UUID> classTeachers(UUID schoolId, UUID sectionId) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (TeacherSection assignment : teacherSectionRepository.findBySectionIdAndSchoolId(sectionId, schoolId)) {
            if (!assignment.isClassTeacher()) {
                continue;
            }
            teacherProfileRepository.findByIdAndSchoolId(assignment.getTeacherId(), schoolId)
                    .ifPresent(profile -> addIfPresent(ids, profile.getUserId()));
        }
        return new ArrayList<>(ids);
    }

    public List<UUID> sectionTeachers(UUID schoolId, UUID classId, UUID sectionId, UUID subjectId) {
        Set<UUID> teacherIds = new LinkedHashSet<>();
        for (TeacherSection assignment : teacherSectionRepository.findBySectionIdAndSchoolId(sectionId, schoolId)) {
            teacherIds.add(assignment.getTeacherId());
        }
        if (classId != null && subjectId != null) {
            for (ClassSubject mapping : classSubjectRepository.findByClassIdAndSchoolId(classId, schoolId)) {
                if (subjectId.equals(mapping.getSubjectId()) && mapping.getTeacherId() != null) {
                    teacherIds.add(mapping.getTeacherId());
                }
            }
        }
        Set<UUID> userIds = new LinkedHashSet<>();
        for (UUID teacherId : teacherIds) {
            teacherProfileRepository.findByIdAndSchoolId(teacherId, schoolId)
                    .ifPresent(profile -> addIfPresent(userIds, profile.getUserId()));
        }
        return new ArrayList<>(userIds);
    }

    public String displayName(UUID userId) {
        if (userId == null) {
            return "Staff";
        }
        return userRepository.findById(userId)
                .map(User::getDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .orElse("Staff");
    }

    private static void addIfPresent(Set<UUID> ids, UUID value) {
        if (value != null) {
            ids.add(value);
        }
    }
}
