package com.schoolms.exam;

import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.SchoolClass;
import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.academics.Section;
import com.schoolms.academics.SectionRepository;
import com.schoolms.academics.StudentEnrollment;
import com.schoolms.academics.StudentEnrollmentRepository;
import com.schoolms.academics.Subject;
import com.schoolms.academics.SubjectRepository;
import com.schoolms.academics.TeacherProfile;
import com.schoolms.academics.TeacherProfileRepository;
import com.schoolms.academics.TeacherSection;
import com.schoolms.academics.TeacherSectionRepository;
import com.schoolms.common.exception.AuthException;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.exam.dto.AdmitCardDto;
import com.schoolms.exam.dto.AdmitCardExportRequest;
import com.schoolms.exam.dto.AdmitCardOptionsDto;
import com.schoolms.exam.dto.AdmitCardSlotDto;
import com.schoolms.exam.dto.AdmitCardStudentDto;
import com.schoolms.file.MinioService;
import com.schoolms.school.School;
import com.schoolms.school.SchoolRepository;
import com.schoolms.security.SecurityUtils;
import com.schoolms.security.UserPrincipal;
import com.schoolms.student.Student;
import com.schoolms.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class AdmitCardService {

    static final List<String> EXAM_TERMS = List.of("QUIZ", "UNIT", "MIDTERM", "TERM", "FINAL", "CONTINUOUS");
    static final List<String> GUIDELINES = List.of(
            "1. Carry this admit card and a valid school ID to every exam.",
            "2. Arrive at least 20 minutes before the scheduled start time.",
            "3. Electronic devices, notes and bags are not allowed inside the exam hall.",
            "4. Follow invigilator instructions. Malpractice leads to disqualification.",
            "5. Sit only in the allotted room. Keep this card visible on the desk."
    );

    private final AcademicYearRepository academicYearRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final ExamScheduleRepository scheduleRepository;
    private final TeacherProfileRepository teacherRepository;
    private final TeacherSectionRepository teacherSectionRepository;
    private final SchoolRepository schoolRepository;
    private final MinioService minioService;
    private final AdmitCardPdfService pdfService;

    @Transactional(readOnly = true)
    public AdmitCardOptionsDto options() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        List<AdmitCardOptionsDto.YearOption> years = academicYearRepository
                .findBySchoolIdOrderByStartDateDesc(schoolId).stream()
                .map(year -> new AdmitCardOptionsDto.YearOption(year.getId(), year.getName(), year.isCurrent()))
                .toList();

        if (isStudent(principal)) {
            Student student = requireOwnStudent(principal);
            UUID yearId = years.stream()
                    .filter(AdmitCardOptionsDto.YearOption::current)
                    .map(AdmitCardOptionsDto.YearOption::id)
                    .findFirst()
                    .orElse(years.isEmpty() ? null : years.get(0).id());
            UUID classId = null;
            UUID sectionId = null;
            List<AdmitCardOptionsDto.ClassOption> classes = List.of();
            if (yearId != null) {
                StudentEnrollment enrollment = enrollmentRepository
                        .findByStudentIdAndAcademicYearId(student.getId(), yearId)
                        .orElse(null);
                if (enrollment != null) {
                    Section section = sectionRepository.findByIdAndSchoolId(enrollment.getSectionId(), schoolId)
                            .orElse(null);
                    if (section != null) {
                        sectionId = section.getId();
                        classId = section.getClassId();
                        SchoolClass klass = classRepository.findByIdAndSchoolId(section.getClassId(), schoolId)
                                .orElse(null);
                        String className = klass == null ? "" : klass.getName();
                        classes = List.of(new AdmitCardOptionsDto.ClassOption(
                                section.getClassId(),
                                className,
                                List.of(new AdmitCardOptionsDto.SectionOption(section.getId(), section.getName()))));
                    }
                }
            }
            return new AdmitCardOptionsDto(years, classes, EXAM_TERMS, true, student.getId(), classId, sectionId);
        }

        TeacherScope scope = teacherScope(schoolId, principal);
        Map<UUID, List<Section>> sectionsByClass = sectionRepository.findBySchoolIdOrderByNameAsc(schoolId)
                .stream()
                .filter(section -> scope.allowsSection(section.getId()))
                .collect(Collectors.groupingBy(Section::getClassId));
        List<AdmitCardOptionsDto.ClassOption> classes = classRepository
                .findBySchoolIdOrderBySortOrderAsc(schoolId).stream()
                .map(klass -> new AdmitCardOptionsDto.ClassOption(
                        klass.getId(),
                        klass.getName(),
                        sectionsByClass.getOrDefault(klass.getId(), List.of()).stream()
                                .map(section -> new AdmitCardOptionsDto.SectionOption(section.getId(), section.getName()))
                                .toList()))
                .filter(option -> !option.sections().isEmpty())
                .toList();
        return new AdmitCardOptionsDto(years, classes, EXAM_TERMS, false, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<AdmitCardStudentDto> roster(UUID academicYearId, UUID classId, UUID sectionId, String examTerm) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        String term = normalizeTerm(examTerm);
        AcademicYear year = academicYearRepository.findByIdAndSchoolId(academicYearId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_year", academicYearId));
        if (isStudent(principal)) {
            Student student = requireOwnStudent(principal);
            AdmitCardDto card = buildCard(schoolId, student, year, term, false);
            return List.of(toStudentDto(card));
        }
        if (sectionId == null) {
            throw new BusinessException("admit_card.section_required");
        }
        Section section = loadSection(schoolId, classId, sectionId);
        assertStaffCanAccessSection(principal, schoolId, section.getId());
        Map<UUID, Integer> examCounts = publishedBySection(schoolId, year.getId(), section.getId(), term).stream()
                .collect(Collectors.groupingBy(ExamSchedule::getSectionId, Collectors.collectingAndThen(Collectors.toList(), List::size)));
        int examCount = examCounts.getOrDefault(section.getId(), 0);
        boolean published = examCount > 0;
        List<Student> students = studentRepository.findBySectionAndYear(schoolId, section.getId(), year.getId());
        Map<UUID, StudentEnrollment> enrollments = enrollmentRepository
                .findBySectionIdAndAcademicYearId(section.getId(), year.getId()).stream()
                .collect(Collectors.toMap(StudentEnrollment::getStudentId, Function.identity(), (a, b) -> a));
        return students.stream()
                .map(student -> {
                    StudentEnrollment enrollment = enrollments.get(student.getId());
                    return new AdmitCardStudentDto(
                            student.getId(),
                            student.getDisplayName(),
                            student.getAdmissionNo(),
                            enrollment == null ? null : enrollment.getRollNumber(),
                            resolvePhoto(student.getPhotoUrl()),
                            published,
                            examCount);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public AdmitCardDto mine(UUID academicYearId, String examTerm) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        Student student = requireOwnStudent(principal);
        AcademicYear year = resolveYear(schoolId, academicYearId);
        return buildCard(schoolId, student, year, normalizeTerm(examTerm), false);
    }

    @Transactional(readOnly = true)
    public AdmitCardDto get(UUID studentId, UUID academicYearId, String examTerm) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        Student student = loadAccessibleStudent(principal, schoolId, studentId);
        AcademicYear year = resolveYear(schoolId, academicYearId);
        return buildCard(schoolId, student, year, normalizeTerm(examTerm), !isStudent(principal));
    }

    @Transactional(readOnly = true)
    public byte[] exportPdf(AdmitCardExportRequest request) {
        return pdfService.render(loadCardsForExport(request));
    }

    @Transactional(readOnly = true)
    public byte[] exportZip(AdmitCardExportRequest request) {
        List<AdmitCardDto> cards = loadCardsForExport(request);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(out)) {
            int index = 1;
            for (AdmitCardDto card : cards) {
                String name = safeFilename(card.admissionNo(), card.studentName(), index++);
                zip.putNextEntry(new ZipEntry(name + ".pdf"));
                zip.write(pdfService.render(List.of(card)));
                zip.closeEntry();
            }
            zip.finish();
            return out.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("admit_card.export_failed");
        }
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

    private List<AdmitCardDto> loadCardsForExport(AdmitCardExportRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        AcademicYear year = resolveYear(schoolId, request.academicYearId());
        String term = normalizeTerm(request.examTerm());
        List<Student> students;
        if (isStudent(principal)) {
            Student own = requireOwnStudent(principal);
            if (request.studentIds() != null && !request.studentIds().isEmpty()
                    && (request.studentIds().size() != 1 || !own.getId().equals(request.studentIds().get(0)))) {
                throw AuthException.accessDenied();
            }
            students = List.of(own);
        } else {
            if (request.sectionId() == null) {
                throw new BusinessException("admit_card.section_required");
            }
            Section section = loadSection(schoolId, request.classId(), request.sectionId());
            assertStaffCanAccessSection(principal, schoolId, section.getId());
            List<Student> roster = studentRepository.findBySectionAndYear(schoolId, section.getId(), year.getId());
            if (request.studentIds() == null || request.studentIds().isEmpty()) {
                students = roster;
            } else {
                Set<UUID> allowed = roster.stream().map(Student::getId).collect(Collectors.toSet());
                LinkedHashMap<UUID, Student> byId = roster.stream()
                        .collect(Collectors.toMap(Student::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
                students = new ArrayList<>();
                for (UUID id : request.studentIds()) {
                    if (!allowed.contains(id)) {
                        throw AuthException.accessDenied();
                    }
                    Student student = byId.get(id);
                    if (student != null) {
                        students.add(student);
                    }
                }
            }
        }
        if (students.isEmpty()) {
            throw new BusinessException("admit_card.no_students");
        }
        List<AdmitCardDto> cards = new ArrayList<>();
        for (Student student : students) {
            AdmitCardDto card = buildCard(schoolId, student, year, term, !isStudent(principal));
            if (!card.published()) {
                throw new BusinessException("admit_card.not_published");
            }
            cards.add(card);
        }
        return cards;
    }

    private AdmitCardDto buildCard(UUID schoolId, Student student, AcademicYear year, String term, boolean staffView) {
        StudentEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndAcademicYearId(student.getId(), year.getId())
                .orElseThrow(() -> new BusinessException("admit_card.not_enrolled"));
        Section section = sectionRepository.findByIdAndSchoolId(enrollment.getSectionId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("section", enrollment.getSectionId()));
        if (staffView) {
            assertStaffCanAccessSection(SecurityUtils.currentPrincipal(), schoolId, section.getId());
        }
        SchoolClass klass = classRepository.findByIdAndSchoolId(section.getClassId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("class", section.getClassId()));
        List<ExamSchedule> published = publishedBySection(schoolId, year.getId(), section.getId(), term);
        Map<UUID, Subject> subjects = subjectRepository.findBySchoolIdOrderByNameAsc(schoolId).stream()
                .collect(Collectors.toMap(Subject::getId, Function.identity()));
        List<AdmitCardSlotDto> slots = published.stream()
                .sorted(Comparator.comparing(ExamSchedule::getExamDate).thenComparing(ExamSchedule::getStartTime))
                .map(schedule -> {
                    Subject subject = subjects.get(schedule.getSubjectId());
                    return new AdmitCardSlotDto(
                            schedule.getId(),
                            subject == null ? "" : subject.getName(),
                            schedule.getExamDate(),
                            schedule.getStartTime(),
                            schedule.getEndTime(),
                            schedule.getRoom());
                })
                .toList();
        School school = schoolRepository.findById(schoolId).orElse(null);
        return new AdmitCardDto(
                student.getId(),
                student.getDisplayName(),
                student.getAdmissionNo(),
                enrollment.getRollNumber(),
                resolvePhoto(student.getPhotoUrl()),
                klass.getId(),
                klass.getName(),
                section.getId(),
                section.getName(),
                year.getId(),
                year.getName(),
                term,
                !slots.isEmpty(),
                school == null ? "" : school.getName(),
                school == null ? "" : school.getAddress(),
                school == null ? "" : school.getPhone(),
                slots,
                GUIDELINES);
    }

    private List<ExamSchedule> publishedBySection(UUID schoolId, UUID yearId, UUID sectionId, String term) {
        return scheduleRepository.findBySchoolIdAndAcademicYearIdOrderByExamDateAscStartTimeAsc(schoolId, yearId)
                .stream()
                .filter(schedule -> "PUBLISHED".equals(schedule.getStatus()))
                .filter(schedule -> sectionId.equals(schedule.getSectionId()))
                .filter(schedule -> term.equals(schedule.getExamTerm()))
                .toList();
    }

    private Student loadAccessibleStudent(UserPrincipal principal, UUID schoolId, UUID studentId) {
        if (isStudent(principal)) {
            Student own = requireOwnStudent(principal);
            if (!own.getId().equals(studentId)) {
                throw AuthException.accessDenied();
            }
            return own;
        }
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("student", studentId));
        return student;
    }

    private Student requireOwnStudent(UserPrincipal principal) {
        if (!isStudent(principal)) {
            throw AuthException.accessDenied();
        }
        return studentRepository.findBySchoolIdAndUserId(principal.schoolId(), principal.id())
                .orElseThrow(AuthException::accessDenied);
    }

    private void assertStaffCanAccessSection(UserPrincipal principal, UUID schoolId, UUID sectionId) {
        if (isStudent(principal)) {
            throw AuthException.accessDenied();
        }
        if (!teacherScope(schoolId, principal).allowsSection(sectionId)) {
            throw AuthException.accessDenied();
        }
    }

    private Section loadSection(UUID schoolId, UUID classId, UUID sectionId) {
        Section section = sectionRepository.findByIdAndSchoolId(sectionId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("section", sectionId));
        if (classId != null && !classId.equals(section.getClassId())) {
            throw new BusinessException("exam_schedule.section_mismatch");
        }
        return section;
    }

    private AcademicYear resolveYear(UUID schoolId, UUID academicYearId) {
        if (academicYearId != null) {
            return academicYearRepository.findByIdAndSchoolId(academicYearId, schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("academic_year", academicYearId));
        }
        return academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId)
                .orElseThrow(() -> new BusinessException("admit_card.year_required"));
    }

    private TeacherScope teacherScope(UUID schoolId, UserPrincipal principal) {
        if (principal.hasRole("ADMIN") || principal.hasRole("SUPER_ADMIN")
                || principal.permissions().contains("EXAM_MANAGE")) {
            return TeacherScope.all();
        }
        TeacherProfile teacher = teacherRepository.findBySchoolIdAndUserId(schoolId, principal.id()).orElse(null);
        if (teacher == null) {
            throw AuthException.accessDenied();
        }
        Set<UUID> sections = teacherSectionRepository.findByTeacherIdAndSchoolId(teacher.getId(), schoolId)
                .stream().map(TeacherSection::getSectionId).collect(Collectors.toSet());
        if (sections.isEmpty()) {
            throw AuthException.accessDenied();
        }
        return new TeacherScope(sections);
    }

    private String resolvePhoto(String photoUrl) {
        if (photoUrl == null || photoUrl.isBlank()) {
            return null;
        }
        if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://") || photoUrl.startsWith("data:")) {
            return photoUrl;
        }
        return minioService.presignedUrl(photoUrl);
    }

    private String normalizeTerm(String examTerm) {
        if (examTerm == null || examTerm.isBlank()) {
            throw new BusinessException("exam_schedule.invalid_term");
        }
        String term = examTerm.trim().toUpperCase(Locale.ROOT);
        if (!EXAM_TERMS.contains(term)) {
            throw new BusinessException("exam_schedule.invalid_term");
        }
        return term;
    }

    private static boolean isStudent(UserPrincipal principal) {
        return principal.hasRole("STUDENT") && !principal.hasRole("ADMIN") && !principal.hasRole("SUPER_ADMIN")
                && !principal.hasRole("TEACHER");
    }

    private static AdmitCardStudentDto toStudentDto(AdmitCardDto card) {
        return new AdmitCardStudentDto(
                card.studentId(),
                card.studentName(),
                card.admissionNo(),
                card.rollNumber(),
                card.photoUrl(),
                card.published(),
                card.datesheet() == null ? 0 : card.datesheet().size());
    }

    private static String safeFilename(String admissionNo, String studentName, int index) {
        String raw = (admissionNo == null ? "student" : admissionNo) + "-" + (studentName == null ? index : studentName);
        String cleaned = raw.replaceAll("[^A-Za-z0-9._-]+", "_");
        if (cleaned.isBlank()) {
            return "admit-card-" + index;
        }
        return cleaned.length() > 60 ? cleaned.substring(0, 60) : cleaned;
    }

    private record TeacherScope(Set<UUID> sections) {
        static TeacherScope all() {
            return new TeacherScope(null);
        }

        boolean allowsSection(UUID sectionId) {
            return sections == null || sections.contains(sectionId);
        }
    }
}
