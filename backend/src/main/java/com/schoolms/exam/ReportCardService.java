package com.schoolms.exam;

import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.GradeBoundary;
import com.schoolms.academics.GradeBoundaryRepository;
import com.schoolms.academics.GradingScheme;
import com.schoolms.academics.GradingSchemeRepository;
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
import com.schoolms.attendance.Attendance;
import com.schoolms.attendance.AttendanceRepository;
import com.schoolms.award.Award;
import com.schoolms.award.AwardRepository;
import com.schoolms.common.enums.AttendanceStatus;
import com.schoolms.common.exception.AuthException;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.exam.dto.ReportCardAttendanceDto;
import com.schoolms.exam.dto.ReportCardBehaviourDto;
import com.schoolms.exam.dto.ReportCardDto;
import com.schoolms.exam.dto.ReportCardExportRequest;
import com.schoolms.exam.dto.ReportCardOptionsDto;
import com.schoolms.exam.dto.ReportCardStudentDto;
import com.schoolms.exam.dto.ReportCardSubjectDto;
import com.schoolms.file.MinioService;
import com.schoolms.school.School;
import com.schoolms.school.SchoolRepository;
import com.schoolms.security.SecurityUtils;
import com.schoolms.security.UserPrincipal;
import com.schoolms.student.Guardian;
import com.schoolms.student.GuardianRepository;
import com.schoolms.student.Student;
import com.schoolms.student.StudentGuardian;
import com.schoolms.student.StudentGuardianRepository;
import com.schoolms.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
public class ReportCardService {

    static final List<String> EXAM_TERMS = List.of("QUIZ", "UNIT", "MIDTERM", "TERM", "FINAL", "CONTINUOUS");
    private static final int COMMENT_MAX = 300;
    private static final String DEFAULT_TEACHER_COMMENT =
            "Shows consistent effort this term. Continue regular revision and class participation.";
    private static final String DEFAULT_PRINCIPAL_COMMENT =
            "Progress noted. Keep working with sincerity and balance studies with co-curricular activities.";

    private final AcademicYearRepository academicYearRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final ExamEntryRepository examEntryRepository;
    private final ExamMarkRepository examMarkRepository;
    private final ReportCardRepository reportCardRepository;
    private final GradingSchemeRepository schemeRepository;
    private final GradeBoundaryRepository boundaryRepository;
    private final TeacherProfileRepository teacherRepository;
    private final TeacherSectionRepository teacherSectionRepository;
    private final SchoolRepository schoolRepository;
    private final AttendanceRepository attendanceRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final GuardianRepository guardianRepository;
    private final AwardRepository awardRepository;
    private final MinioService minioService;
    private final ReportCardPdfService pdfService;

    @Transactional(readOnly = true)
    public ReportCardOptionsDto options() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        List<ReportCardOptionsDto.YearOption> years = academicYearRepository
                .findBySchoolIdOrderByStartDateDesc(schoolId).stream()
                .map(year -> new ReportCardOptionsDto.YearOption(year.getId(), year.getName(), year.isCurrent()))
                .toList();

        if (isLearner(principal)) {
            Student student = requireOwnStudent(principal);
            UUID yearId = years.stream()
                    .filter(ReportCardOptionsDto.YearOption::current)
                    .map(ReportCardOptionsDto.YearOption::id)
                    .findFirst()
                    .orElse(years.isEmpty() ? null : years.get(0).id());
            UUID classId = null;
            UUID sectionId = null;
            List<ReportCardOptionsDto.ClassOption> classes = List.of();
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
                        classes = List.of(new ReportCardOptionsDto.ClassOption(
                                section.getClassId(),
                                className,
                                List.of(new ReportCardOptionsDto.SectionOption(section.getId(), section.getName()))));
                    }
                }
            }
            return new ReportCardOptionsDto(years, classes, EXAM_TERMS, true, student.getId(), classId, sectionId);
        }

        TeacherScope scope = teacherScope(schoolId, principal);
        Map<UUID, List<Section>> sectionsByClass = sectionRepository.findBySchoolIdOrderByNameAsc(schoolId)
                .stream()
                .filter(section -> scope.allowsSection(section.getId()))
                .collect(Collectors.groupingBy(Section::getClassId));
        List<ReportCardOptionsDto.ClassOption> classes = classRepository
                .findBySchoolIdOrderBySortOrderAsc(schoolId).stream()
                .map(klass -> new ReportCardOptionsDto.ClassOption(
                        klass.getId(),
                        klass.getName(),
                        sectionsByClass.getOrDefault(klass.getId(), List.of()).stream()
                                .map(section -> new ReportCardOptionsDto.SectionOption(section.getId(), section.getName()))
                                .toList()))
                .filter(option -> !option.sections().isEmpty())
                .toList();
        return new ReportCardOptionsDto(years, classes, EXAM_TERMS, false, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<ReportCardStudentDto> roster(UUID academicYearId, UUID classId, UUID sectionId, String examTerm) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        String term = normalizeTerm(examTerm);
        AcademicYear year = academicYearRepository.findByIdAndSchoolId(academicYearId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_year", academicYearId));
        if (isLearner(principal)) {
            Student student = requireOwnStudent(principal);
            ReportCardDto card = buildCard(schoolId, student, year, term, false);
            return List.of(toStudentDto(card));
        }
        if (sectionId == null) {
            throw new BusinessException("report_card.section_required");
        }
        Section section = loadSection(schoolId, classId, sectionId);
        assertStaffCanAccessSection(principal, schoolId, section.getId());
        SectionContext ctx = loadSectionContext(schoolId, year, section, term);
        return ctx.students.stream()
                .map(student -> toStudentDto(assemble(ctx, student, false)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReportCardDto mine(UUID academicYearId, String examTerm) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        Student student = requireOwnStudent(principal);
        AcademicYear year = resolveYear(schoolId, academicYearId);
        return buildCard(schoolId, student, year, normalizeTerm(examTerm), false);
    }

    @Transactional(readOnly = true)
    public ReportCardDto get(UUID studentId, UUID academicYearId, String examTerm) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        Student student = loadAccessibleStudent(principal, schoolId, studentId);
        AcademicYear year = resolveYear(schoolId, academicYearId);
        return buildCard(schoolId, student, year, normalizeTerm(examTerm), !isLearner(principal));
    }

    @Transactional(readOnly = true)
    public byte[] exportPdf(ReportCardExportRequest request) {
        return pdfService.render(loadCardsForExport(request));
    }

    @Transactional(readOnly = true)
    public byte[] exportZip(ReportCardExportRequest request) {
        List<ReportCardDto> cards = loadCardsForExport(request);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(out)) {
            int index = 1;
            for (ReportCardDto card : cards) {
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
            throw new BusinessException("report_card.export_failed");
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

    private List<ReportCardDto> loadCardsForExport(ReportCardExportRequest request) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        AcademicYear year = resolveYear(schoolId, request.academicYearId());
        String term = normalizeTerm(request.examTerm());
        List<Student> students;
        SectionContext ctx;
        if (isLearner(principal)) {
            Student own = requireOwnStudent(principal);
            if (request.studentIds() != null && !request.studentIds().isEmpty()
                    && (request.studentIds().size() != 1 || !own.getId().equals(request.studentIds().get(0)))) {
                throw AuthException.accessDenied();
            }
            students = List.of(own);
            ctx = contextForStudent(schoolId, own, year, term, false);
        } else {
            if (request.sectionId() == null) {
                throw new BusinessException("report_card.section_required");
            }
            Section section = loadSection(schoolId, request.classId(), request.sectionId());
            assertStaffCanAccessSection(principal, schoolId, section.getId());
            ctx = loadSectionContext(schoolId, year, section, term);
            List<Student> roster = ctx.students;
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
            throw new BusinessException("report_card.no_students");
        }
        List<ReportCardDto> cards = new ArrayList<>();
        for (Student student : students) {
            ReportCardDto card = assemble(ctx, student, !isLearner(principal));
            if (!card.published()) {
                throw new BusinessException("report_card.not_published");
            }
            cards.add(card);
        }
        return cards;
    }

    private SectionContext contextForStudent(UUID schoolId, Student student, AcademicYear year, String term,
                                             boolean staffView) {
        StudentEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndAcademicYearId(student.getId(), year.getId())
                .orElseThrow(() -> new BusinessException("report_card.not_enrolled"));
        Section section = sectionRepository.findByIdAndSchoolId(enrollment.getSectionId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("section", enrollment.getSectionId()));
        if (staffView) {
            assertStaffCanAccessSection(SecurityUtils.currentPrincipal(), schoolId, section.getId());
        }
        return loadSectionContext(schoolId, year, section, term);
    }

    private ReportCardDto buildCard(UUID schoolId, Student student, AcademicYear year, String term, boolean staffView) {
        return assemble(contextForStudent(schoolId, student, year, term, staffView), student, staffView);
    }

    private SectionContext loadSectionContext(UUID schoolId, AcademicYear year, Section section, String term) {
        SchoolClass klass = classRepository.findByIdAndSchoolId(section.getClassId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("class", section.getClassId()));
        List<Student> students = studentRepository.findBySectionAndYear(schoolId, section.getId(), year.getId());
        Map<UUID, StudentEnrollment> enrollments = enrollmentRepository
                .findBySectionIdAndAcademicYearId(section.getId(), year.getId()).stream()
                .collect(Collectors.toMap(StudentEnrollment::getStudentId, Function.identity(), (a, b) -> a));
        List<ExamEntry> entries = examEntryRepository
                .findBySchoolIdAndAcademicYearIdAndSectionIdAndExamTerm(schoolId, year.getId(), section.getId(), term);
        Map<UUID, Subject> subjects = subjectRepository.findBySchoolIdOrderByNameAsc(schoolId).stream()
                .collect(Collectors.toMap(Subject::getId, Function.identity()));
        List<UUID> entryIds = entries.stream().map(ExamEntry::getId).toList();
        Map<UUID, List<ExamMark>> marksByStudent = entryIds.isEmpty()
                ? Map.of()
                : examMarkRepository.findBySchoolIdAndExamEntryIdIn(schoolId, entryIds).stream()
                        .collect(Collectors.groupingBy(ExamMark::getStudentId));
        Map<UUID, ExamEntry> entriesById = entries.stream()
                .collect(Collectors.toMap(ExamEntry::getId, Function.identity()));
        Map<UUID, ReportCard> remarks = reportCardRepository
                .findBySchoolIdAndAcademicYearIdAndSectionIdAndExamTerm(schoolId, year.getId(), section.getId(), term)
                .stream()
                .collect(Collectors.toMap(ReportCard::getStudentId, Function.identity(), (a, b) -> a));
        GradingScheme scheme = schemeRepository
                .findBySchoolIdAndAcademicYearIdAndClassIdAndStatus(schoolId, year.getId(), klass.getId(), "ACTIVE")
                .orElse(null);
        List<GradeBoundary> boundaries = scheme == null
                ? List.of()
                : boundaryRepository.findBySchemeIdAndSchoolIdOrderBySortOrderAsc(scheme.getId(), schoolId);
        String classTeacher = classTeacherName(schoolId, section.getId(), year.getId());
        School school = schoolRepository.findById(schoolId).orElse(null);
        LocalDate from = year.getStartDate();
        LocalDate to = year.getEndDate() == null ? LocalDate.now() : year.getEndDate();
        if (to.isAfter(LocalDate.now())) {
            to = LocalDate.now();
        }
        List<UUID> studentIds = students.stream().map(Student::getId).toList();
        Map<UUID, List<Attendance>> attendanceByStudent = studentIds.isEmpty()
                ? Map.of()
                : attendanceRepository.findByStudentIdInAndAttendanceDateBetween(studentIds, from, to).stream()
                        .collect(Collectors.groupingBy(Attendance::getStudentId));
        Map<UUID, String> guardians = guardianNames(schoolId, studentIds);
        Map<UUID, List<Award>> awards = studentIds.isEmpty()
                ? Map.of()
                : awardRepository.findBySchoolIdOrderByAwardedDateDesc(schoolId).stream()
                        .filter(award -> award.getStudentId() != null && studentIds.contains(award.getStudentId()))
                        .collect(Collectors.groupingBy(Award::getStudentId));
        SectionContext ctx = new SectionContext();
        ctx.schoolId = schoolId;
        ctx.year = year;
        ctx.klass = klass;
        ctx.section = section;
        ctx.term = term;
        ctx.students = students;
        ctx.enrollments = enrollments;
        ctx.entries = entries;
        ctx.entriesById = entriesById;
        ctx.subjects = subjects;
        ctx.marksByStudent = marksByStudent;
        ctx.remarks = remarks;
        ctx.scheme = scheme;
        ctx.boundaries = boundaries;
        ctx.classTeacher = classTeacher;
        ctx.school = school;
        ctx.from = from;
        ctx.to = to;
        ctx.attendanceByStudent = attendanceByStudent;
        ctx.guardians = guardians;
        ctx.awards = awards;
        return ctx;
    }

    private ReportCardDto assemble(SectionContext ctx, Student student, boolean staffView) {
        StudentEnrollment enrollment = ctx.enrollments.get(student.getId());
        List<ExamMark> marks = ctx.marksByStudent.getOrDefault(student.getId(), List.of());
        List<ReportCardSubjectDto> subjects = ctx.entries.stream()
                .sorted(Comparator.comparing(entry -> {
                    Subject subject = ctx.subjects.get(entry.getSubjectId());
                    return subject == null ? "" : subject.getName();
                }))
                .map(entry -> toSubjectRow(entry, marks, ctx))
                .toList();
        boolean ready = !subjects.isEmpty() && subjects.stream().allMatch(row -> row.total() != null);
        BigDecimal obtained = subjects.stream()
                .map(row -> nz(row.total()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal max = subjects.stream()
                .map(row -> nz(row.maxTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal percent = max.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : obtained.multiply(new BigDecimal("100")).divide(max, 2, RoundingMode.HALF_UP);
        String grade = letterGrade(percent, ctx.boundaries);
        String result = ctx.scheme == null || percent.compareTo(ctx.scheme.getPassPercent()) >= 0 ? "PASS" : "FAIL";
        ReportCard stored = ctx.remarks.get(student.getId());
        boolean published = stored != null && stored.isPublished();
        if (ready && stored == null && staffView) {
            published = false;
        }
        ReportCardAttendanceDto attendance = attendanceSummary(
                ctx.attendanceByStudent.getOrDefault(student.getId(), List.of()), ctx.from, ctx.to);
        String teacherComment = clip(stored == null ? DEFAULT_TEACHER_COMMENT : stored.getTeacherComment());
        String principalComment = clip(stored == null ? DEFAULT_PRINCIPAL_COMMENT : stored.getPrincipalComment());
        String coCurricular = stored == null || stored.getCoCurricular() == null || stored.getCoCurricular().isBlank()
                ? awardsText(ctx.awards.getOrDefault(student.getId(), List.of()))
                : clip(stored.getCoCurricular());
        ReportCardBehaviourDto behaviour = new ReportCardBehaviourDto(
                stored == null ? "GOOD" : stored.getBehaviourConduct(),
                stored == null ? "GOOD" : stored.getBehaviourDiscipline(),
                stored == null ? "GOOD" : stored.getBehaviourPunctuality(),
                coCurricular);
        School school = ctx.school;
        return new ReportCardDto(
                student.getId(),
                student.getDisplayName(),
                student.getAdmissionNo(),
                enrollment == null ? null : enrollment.getRollNumber(),
                student.getDateOfBirth(),
                ctx.guardians.getOrDefault(student.getId(), ""),
                resolvePhoto(student.getPhotoUrl()),
                ctx.klass.getId(),
                ctx.klass.getName(),
                ctx.section.getId(),
                ctx.section.getName(),
                ctx.year.getId(),
                ctx.year.getName(),
                ctx.term,
                ready,
                published,
                school == null ? "" : school.getName(),
                school == null ? "" : blank(school.getAddress()),
                school == null ? "" : blank(school.getPhone()),
                school == null ? "" : blank(school.getEmail()),
                school == null ? "" : affiliation(school),
                ctx.classTeacher,
                obtained,
                max,
                percent,
                grade,
                result,
                attendance,
                subjects,
                behaviour,
                teacherComment,
                principalComment);
    }

    private ReportCardSubjectDto toSubjectRow(ExamEntry entry, List<ExamMark> marks, SectionContext ctx) {
        Subject subject = ctx.subjects.get(entry.getSubjectId());
        ExamMark mark = marks.stream()
                .filter(row -> entry.getId().equals(row.getExamEntryId()))
                .findFirst()
                .orElse(null);
        BigDecimal maxTotal = nz(entry.getMaxTheory()).add(nz(entry.getMaxPractical())).add(nz(entry.getMaxAssignment()));
        return new ReportCardSubjectDto(
                entry.getSubjectId(),
                subject == null ? "" : subject.getName(),
                subject == null ? "" : subject.getCode(),
                mark == null ? null : mark.getTheory(),
                entry.getMaxTheory(),
                mark == null ? null : mark.getPractical(),
                entry.getMaxPractical(),
                mark == null ? null : mark.getAssignment(),
                entry.getMaxAssignment(),
                mark == null ? null : mark.getTotal(),
                maxTotal,
                mark == null ? null : mark.getPercentage(),
                mark == null ? null : mark.getGradeLabel(),
                mark == null ? null : mark.getRemarks());
    }

    private ReportCardAttendanceDto attendanceSummary(List<Attendance> rows, LocalDate from, LocalDate to) {
        long present = 0;
        long absent = 0;
        long late = 0;
        long leave = 0;
        for (Attendance row : rows) {
            AttendanceStatus status = row.getStatus();
            if (status == AttendanceStatus.PRESENT) {
                present++;
            } else if (status == AttendanceStatus.ABSENT) {
                absent++;
            } else if (status == AttendanceStatus.LATE) {
                late++;
            } else if (status == AttendanceStatus.LEAVE) {
                leave++;
            }
        }
        long working = present + absent + late + leave;
        long countedPresent = present + late;
        BigDecimal percent = working == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(countedPresent * 100.0 / working).setScale(2, RoundingMode.HALF_UP);
        return new ReportCardAttendanceDto(working, countedPresent, absent, late, leave, percent, from, to);
    }

    private Map<UUID, String> guardianNames(UUID schoolId, List<UUID> studentIds) {
        Map<UUID, String> names = new HashMap<>();
        for (UUID studentId : studentIds) {
            List<StudentGuardian> links = studentGuardianRepository.findWithGuardians(schoolId, studentId);
            String name = links.stream()
                    .sorted(Comparator.comparing((StudentGuardian link) -> !link.isPrimary()))
                    .map(StudentGuardian::getGuardian)
                    .filter(guardian -> guardian != null)
                    .map(Guardian::getDisplayName)
                    .findFirst()
                    .orElse("");
            names.put(studentId, name);
        }
        return names;
    }

    private String classTeacherName(UUID schoolId, UUID sectionId, UUID yearId) {
        return teacherSectionRepository.findBySectionIdAndSchoolId(sectionId, schoolId).stream()
                .filter(TeacherSection::isClassTeacher)
                .filter(link -> yearId.equals(link.getAcademicYearId()))
                .map(TeacherSection::getTeacherId)
                .findFirst()
                .flatMap(id -> teacherRepository.findByIdAndSchoolId(id, schoolId))
                .map(TeacherProfile::getDisplayName)
                .orElse("");
    }

    private String letterGrade(BigDecimal percent, List<GradeBoundary> boundaries) {
        if (percent == null || boundaries == null || boundaries.isEmpty()) {
            return null;
        }
        for (GradeBoundary boundary : boundaries) {
            if (percent.compareTo(boundary.getMinPercent()) >= 0
                    && percent.compareTo(boundary.getMaxPercent()) <= 0) {
                return boundary.getLabel();
            }
        }
        return boundaries.get(boundaries.size() - 1).getLabel();
    }

    private Student loadAccessibleStudent(UserPrincipal principal, UUID schoolId, UUID studentId) {
        if (isLearner(principal)) {
            Student own = requireOwnStudent(principal);
            if (!own.getId().equals(studentId)) {
                throw AuthException.accessDenied();
            }
            return own;
        }
        return studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("student", studentId));
    }

    private Student requireOwnStudent(UserPrincipal principal) {
        if (principal.hasRole("PARENT") && !principal.hasRole("STUDENT")
                && !principal.hasRole("ADMIN") && !principal.hasRole("SUPER_ADMIN")
                && !principal.hasRole("TEACHER")) {
            Guardian guardian = guardianRepository.findBySchoolIdAndUserId(principal.schoolId(), principal.id())
                    .orElseThrow(AuthException::accessDenied);
            List<StudentGuardian> links = studentGuardianRepository.findWithStudents(principal.schoolId(), guardian.getId());
            if (links.isEmpty() || links.get(0).getStudent() == null) {
                throw AuthException.accessDenied();
            }
            return links.get(0).getStudent();
        }
        if (!principal.hasRole("STUDENT")) {
            throw AuthException.accessDenied();
        }
        return studentRepository.findBySchoolIdAndUserId(principal.schoolId(), principal.id())
                .orElseThrow(AuthException::accessDenied);
    }

    private void assertStaffCanAccessSection(UserPrincipal principal, UUID schoolId, UUID sectionId) {
        if (isLearner(principal)) {
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
                .orElseThrow(() -> new BusinessException("report_card.year_required"));
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

    private static boolean isLearner(UserPrincipal principal) {
        return (principal.hasRole("STUDENT") || principal.hasRole("PARENT"))
                && !principal.hasRole("ADMIN") && !principal.hasRole("SUPER_ADMIN")
                && !principal.hasRole("TEACHER");
    }

    private static ReportCardStudentDto toStudentDto(ReportCardDto card) {
        return new ReportCardStudentDto(
                card.studentId(),
                card.studentName(),
                card.admissionNo(),
                card.rollNumber(),
                card.photoUrl(),
                card.ready(),
                card.published(),
                card.percentage(),
                card.overallGrade());
    }

    private static String awardsText(List<Award> awards) {
        if (awards == null || awards.isEmpty()) {
            return "";
        }
        return clip(awards.stream().map(Award::getTitle).collect(Collectors.joining("; ")));
    }

    private static String affiliation(School school) {
        String code = school.getCode() == null ? "" : school.getCode();
        if (code.isBlank()) {
            return "Recognised school";
        }
        return "Affiliation / School code: " + code;
    }

    private static String clip(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() > COMMENT_MAX ? trimmed.substring(0, COMMENT_MAX) : trimmed;
    }

    private static String blank(String value) {
        return value == null ? "" : value;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String safeFilename(String admissionNo, String studentName, int index) {
        String raw = (admissionNo == null ? "student" : admissionNo) + "-" + (studentName == null ? index : studentName);
        String cleaned = raw.replaceAll("[^A-Za-z0-9._-]+", "_");
        if (cleaned.isBlank()) {
            return "report-card-" + index;
        }
        return cleaned.length() > 60 ? cleaned.substring(0, 60) : cleaned;
    }

    private static class SectionContext {
        UUID schoolId;
        AcademicYear year;
        SchoolClass klass;
        Section section;
        String term;
        List<Student> students;
        Map<UUID, StudentEnrollment> enrollments;
        List<ExamEntry> entries;
        Map<UUID, ExamEntry> entriesById;
        Map<UUID, Subject> subjects;
        Map<UUID, List<ExamMark>> marksByStudent;
        Map<UUID, ReportCard> remarks;
        GradingScheme scheme;
        List<GradeBoundary> boundaries;
        String classTeacher;
        School school;
        LocalDate from;
        LocalDate to;
        Map<UUID, List<Attendance>> attendanceByStudent;
        Map<UUID, String> guardians;
        Map<UUID, List<Award>> awards;
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
