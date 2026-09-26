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
import com.schoolms.common.enums.Gender;
import com.schoolms.common.exception.AuthException;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.exam.dto.MarksheetActionRequest;
import com.schoolms.exam.dto.MarksheetDto;
import com.schoolms.exam.dto.MarksheetExportRequest;
import com.schoolms.exam.dto.MarksheetOptionsDto;
import com.schoolms.exam.dto.MarksheetPublishRequest;
import com.schoolms.exam.dto.MarksheetStudentDto;
import com.schoolms.exam.dto.MarksheetSubjectDto;
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
import java.time.Instant;
import java.time.LocalDate;
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
public class MarksheetService {

    static final List<String> EXAM_TERMS = List.of("QUIZ", "UNIT", "MIDTERM", "TERM", "FINAL", "CONTINUOUS");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final AcademicYearRepository academicYearRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final ExamEntryRepository examEntryRepository;
    private final ExamMarkRepository examMarkRepository;
    private final MarksheetRepository marksheetRepository;
    private final GradingSchemeRepository schemeRepository;
    private final GradeBoundaryRepository boundaryRepository;
    private final TeacherProfileRepository teacherRepository;
    private final TeacherSectionRepository teacherSectionRepository;
    private final SchoolRepository schoolRepository;
    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final MinioService minioService;
    private final MarksheetPdfService pdfService;

    @Transactional(readOnly = true)
    public MarksheetOptionsDto options() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        List<MarksheetOptionsDto.YearOption> years = academicYearRepository
                .findBySchoolIdOrderByStartDateDesc(schoolId).stream()
                .map(year -> new MarksheetOptionsDto.YearOption(year.getId(), year.getName(), year.isCurrent()))
                .toList();
        boolean manage = canManage(principal);

        if (isLearner(principal)) {
            Student student = requireOwnStudent(principal);
            UUID yearId = years.stream()
                    .filter(MarksheetOptionsDto.YearOption::current)
                    .map(MarksheetOptionsDto.YearOption::id)
                    .findFirst()
                    .orElse(years.isEmpty() ? null : years.get(0).id());
            UUID classId = null;
            UUID sectionId = null;
            List<MarksheetOptionsDto.ClassOption> classes = List.of();
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
                        classes = List.of(new MarksheetOptionsDto.ClassOption(
                                section.getClassId(),
                                className,
                                List.of(new MarksheetOptionsDto.SectionOption(section.getId(), section.getName()))));
                    }
                }
            }
            return new MarksheetOptionsDto(years, classes, EXAM_TERMS, true, false, false, student.getId(), classId, sectionId);
        }

        TeacherScope scope = teacherScope(schoolId, principal);
        Map<UUID, List<Section>> sectionsByClass = sectionRepository.findBySchoolIdOrderByNameAsc(schoolId)
                .stream()
                .filter(section -> scope.allowsSection(section.getId()))
                .collect(Collectors.groupingBy(Section::getClassId));
        List<MarksheetOptionsDto.ClassOption> classes = classRepository
                .findBySchoolIdOrderBySortOrderAsc(schoolId).stream()
                .map(klass -> new MarksheetOptionsDto.ClassOption(
                        klass.getId(),
                        klass.getName(),
                        sectionsByClass.getOrDefault(klass.getId(), List.of()).stream()
                                .map(section -> new MarksheetOptionsDto.SectionOption(section.getId(), section.getName()))
                                .toList()))
                .filter(option -> !option.sections().isEmpty())
                .toList();
        boolean submit = canSubmit(principal);
        return new MarksheetOptionsDto(years, classes, EXAM_TERMS, false, manage, submit, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<MarksheetStudentDto> roster(UUID academicYearId, UUID classId, UUID sectionId, String examTerm,
                                            String query, String status) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        String term = normalizeTerm(examTerm);
        AcademicYear year = academicYearRepository.findByIdAndSchoolId(academicYearId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_year", academicYearId));
        String statusFilter = normalizeStatusFilter(status);
        if (isLearner(principal)) {
            Student student = requireOwnStudent(principal);
            MarksheetDto card = buildCard(schoolId, student, year, term);
            if (statusFilter != null && !statusFilter.equals(card.status())) {
                return List.of();
            }
            return List.of(toStudentDto(card));
        }
        if (sectionId == null) {
            throw new BusinessException("marksheet.section_required");
        }
        Section section = loadSection(schoolId, classId, sectionId);
        assertStaffCanAccessSection(principal, schoolId, section.getId());
        SectionContext ctx = loadSectionContext(schoolId, year, section, term);
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return ctx.students.stream()
                .filter(student -> matchesQuery(student, needle))
                .map(student -> toStudentDto(assemble(ctx, student)))
                .filter(row -> statusFilter == null || statusFilter.equals(row.status()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MarksheetDto mine(UUID academicYearId, String examTerm) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        Student student = requireOwnStudent(principal);
        AcademicYear year = resolveYear(schoolId, academicYearId);
        return buildCard(schoolId, student, year, normalizeTerm(examTerm));
    }

    @Transactional(readOnly = true)
    public MarksheetDto get(UUID studentId, UUID academicYearId, String examTerm) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        Student student = loadAccessibleStudent(principal, schoolId, studentId);
        AcademicYear year = resolveYear(schoolId, academicYearId);
        return buildCard(schoolId, student, year, normalizeTerm(examTerm));
    }

    @Transactional
    public List<MarksheetStudentDto> submit(MarksheetActionRequest request) {
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        if (!canSubmit(principal)) {
            throw AuthException.accessDenied();
        }
        SectionContext ctx = loadActionContext(request.academicYearId(), request.classId(), request.sectionId(),
                request.examTerm());
        List<Student> students = selectStudents(ctx, request.studentIds());
        Instant now = Instant.now();
        UUID actor = principal.id();
        for (Student student : students) {
            MarksheetDto card = assemble(ctx, student);
            if (!card.ready()) {
                throw new BusinessException("marksheet.not_ready");
            }
            String status = card.status();
            if (!"DRAFT".equals(status) && !"REJECTED".equals(status)) {
                throw new BusinessException("marksheet.invalid_transition");
            }
            if (card.locked()) {
                throw new BusinessException("marksheet.locked");
            }
            Marksheet stored = ctx.records.computeIfAbsent(student.getId(), id -> newRecord(ctx, student));
            stored.setStatus("PENDING_APPROVAL");
            stored.setPublished(false);
            stored.setLocked(false);
            stored.setSubmittedAt(now);
            stored.setSubmittedBy(actor);
            stored.setRejectedAt(null);
            stored.setRejectedBy(null);
            stored.setRejectionReason(null);
            stored.setPublishedAt(null);
            stored.setPublishedBy(null);
            stored.setLockedAt(null);
            stored.setLockedBy(null);
            if (stored.getSerialNo() == null || stored.getSerialNo().isBlank()) {
                stored.setSerialNo(serialFor(ctx, student));
            }
            marksheetRepository.save(stored);
            ctx.records.put(student.getId(), stored);
        }
        return students.stream().map(student -> toStudentDto(assemble(ctx, student))).toList();
    }

    @Transactional
    public List<MarksheetStudentDto> approve(MarksheetActionRequest request) {
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        if (!canManage(principal)) {
            throw AuthException.accessDenied();
        }
        SectionContext ctx = loadActionContext(request.academicYearId(), request.classId(), request.sectionId(),
                request.examTerm());
        List<Student> students = selectStudents(ctx, request.studentIds());
        Instant now = Instant.now();
        UUID actor = principal.id();
        ensureRanks(ctx);
        for (Student student : students) {
            MarksheetDto card = assemble(ctx, student);
            if (!"PENDING_APPROVAL".equals(card.status())) {
                throw new BusinessException("marksheet.invalid_transition");
            }
            if (!card.ready()) {
                throw new BusinessException("marksheet.not_ready");
            }
            Marksheet stored = ctx.records.computeIfAbsent(student.getId(), id -> newRecord(ctx, student));
            applyPublished(stored, ctx, student, now, actor, request.locked(), ctx.ranks.get(student.getId()));
            marksheetRepository.save(stored);
            ctx.records.put(student.getId(), stored);
        }
        return students.stream().map(student -> toStudentDto(assemble(ctx, student))).toList();
    }

    @Transactional
    public List<MarksheetStudentDto> reject(MarksheetActionRequest request) {
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        if (!canManage(principal)) {
            throw AuthException.accessDenied();
        }
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (reason.isBlank()) {
            throw new BusinessException("marksheet.reason_required");
        }
        SectionContext ctx = loadActionContext(request.academicYearId(), request.classId(), request.sectionId(),
                request.examTerm());
        List<Student> students = selectStudents(ctx, request.studentIds());
        Instant now = Instant.now();
        UUID actor = principal.id();
        for (Student student : students) {
            Marksheet stored = ctx.records.get(student.getId());
            String status = stored == null ? "DRAFT" : stored.getStatus();
            if (!"PENDING_APPROVAL".equals(status)) {
                throw new BusinessException("marksheet.invalid_transition");
            }
            stored.setStatus("REJECTED");
            stored.setPublished(false);
            stored.setLocked(false);
            stored.setRejectedAt(now);
            stored.setRejectedBy(actor);
            stored.setRejectionReason(reason);
            stored.setPublishedAt(null);
            stored.setPublishedBy(null);
            stored.setLockedAt(null);
            stored.setLockedBy(null);
            stored.setClassRank(null);
            marksheetRepository.save(stored);
            ctx.records.put(student.getId(), stored);
            resetMarksToDraft(ctx, student.getId());
        }
        return students.stream().map(student -> toStudentDto(assemble(ctx, student))).toList();
    }

    @Transactional
    public List<MarksheetStudentDto> publish(MarksheetPublishRequest request) {
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        if (!canManage(principal)) {
            throw AuthException.accessDenied();
        }
        UUID schoolId = SecurityUtils.currentSchoolId();
        AcademicYear year = resolveYear(schoolId, request.academicYearId());
        String term = normalizeTerm(request.examTerm());
        if (request.sectionId() == null) {
            throw new BusinessException("marksheet.section_required");
        }
        Section section = loadSection(schoolId, request.classId(), request.sectionId());
        SectionContext ctx = loadSectionContext(schoolId, year, section, term);
        List<Student> students = selectStudents(ctx, request.studentIds());
        Instant now = Instant.now();
        UUID actor = principal.id();
        if (request.published()) {
            ensureRanks(ctx);
        }
        for (Student student : students) {
            Marksheet stored = ctx.records.computeIfAbsent(student.getId(), id -> newRecord(ctx, student));
            if (request.published()) {
                MarksheetDto card = assemble(ctx, student);
                if (!card.ready()) {
                    throw new BusinessException("marksheet.not_ready");
                }
                String status = stored.getStatus() == null ? "DRAFT" : stored.getStatus();
                if (!"PENDING_APPROVAL".equals(status) && !"PUBLISHED".equals(status)) {
                    throw new BusinessException("marksheet.invalid_transition");
                }
                applyPublished(stored, ctx, student, now, actor, request.locked(), ctx.ranks.get(student.getId()));
            } else {
                if (stored.isLocked() && !"PUBLISHED".equals(stored.getStatus())) {
                    throw new BusinessException("marksheet.locked");
                }
                stored.setStatus("DRAFT");
                stored.setPublished(false);
                stored.setLocked(false);
                stored.setPublishedAt(null);
                stored.setPublishedBy(null);
                stored.setLockedAt(null);
                stored.setLockedBy(null);
                stored.setClassRank(null);
            }
            marksheetRepository.save(stored);
            ctx.records.put(student.getId(), stored);
        }
        return students.stream().map(student -> toStudentDto(assemble(ctx, student))).toList();
    }

    @Transactional(readOnly = true)
    public byte[] exportPdf(MarksheetExportRequest request) {
        return exportPdfFile(request).body();
    }

    @Transactional(readOnly = true)
    public ExportFile exportPdfFile(MarksheetExportRequest request) {
        List<MarksheetDto> cards = loadCardsForExport(request);
        String filename = cards.size() == 1 ? downloadFilename(cards.get(0)) : "marksheets.pdf";
        return new ExportFile(filename, pdfService.render(cards));
    }

    public record ExportFile(String filename, byte[] body) {
    }

    @Transactional(readOnly = true)
    public byte[] exportZip(MarksheetExportRequest request) {
        List<MarksheetDto> cards = loadCardsForExport(request);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(out)) {
            for (MarksheetDto card : cards) {
                zip.putNextEntry(new ZipEntry(downloadFilename(card)));
                zip.write(pdfService.render(List.of(card)));
                zip.closeEntry();
            }
            zip.finish();
            return out.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("marksheet.export_failed");
        }
    }

    public String downloadFilename(MarksheetDto card) {
        String roll = card.rollNumber() == null ? "NA" : String.valueOf(card.rollNumber());
        String term = card.examTerm() == null ? "TERM" : card.examTerm();
        return "Marksheet_" + roll + "_" + term + ".pdf";
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

    public boolean sectionTermLocked(UUID schoolId, UUID academicYearId, UUID sectionId, String examTerm) {
        return marksheetRepository
                .findBySchoolIdAndAcademicYearIdAndSectionIdAndExamTerm(schoolId, academicYearId, sectionId, examTerm)
                .stream()
                .anyMatch(row -> row.isLocked()
                        || "PENDING_APPROVAL".equals(row.getStatus())
                        || "PUBLISHED".equals(row.getStatus()));
    }

    private List<MarksheetDto> loadCardsForExport(MarksheetExportRequest request) {
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
                throw new BusinessException("marksheet.section_required");
            }
            Section section = loadSection(schoolId, request.classId(), request.sectionId());
            assertStaffCanAccessSection(principal, schoolId, section.getId());
            ctx = loadSectionContext(schoolId, year, section, term);
            students = selectStudents(ctx, request.studentIds());
        }
        if (students.isEmpty()) {
            throw new BusinessException("marksheet.no_students");
        }
        List<MarksheetDto> cards = new ArrayList<>();
        for (Student student : students) {
            MarksheetDto card = assemble(ctx, student);
            if (!card.published()) {
                throw new BusinessException("marksheet.not_published");
            }
            cards.add(card);
        }
        return cards;
    }

    private List<Student> selectStudents(SectionContext ctx, List<UUID> requested) {
        List<Student> roster = ctx.students;
        if (requested == null || requested.isEmpty()) {
            return roster;
        }
        Set<UUID> allowed = roster.stream().map(Student::getId).collect(Collectors.toSet());
        LinkedHashMap<UUID, Student> byId = roster.stream()
                .collect(Collectors.toMap(Student::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        List<Student> students = new ArrayList<>();
        for (UUID id : requested) {
            if (!allowed.contains(id)) {
                throw AuthException.accessDenied();
            }
            Student student = byId.get(id);
            if (student != null) {
                students.add(student);
            }
        }
        return students;
    }

    private SectionContext contextForStudent(UUID schoolId, Student student, AcademicYear year, String term,
                                             boolean staffView) {
        StudentEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndAcademicYearId(student.getId(), year.getId())
                .orElseThrow(() -> new BusinessException("marksheet.not_enrolled"));
        Section section = sectionRepository.findByIdAndSchoolId(enrollment.getSectionId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("section", enrollment.getSectionId()));
        if (staffView) {
            assertStaffCanAccessSection(SecurityUtils.currentPrincipal(), schoolId, section.getId());
        }
        return loadSectionContext(schoolId, year, section, term);
    }

    private MarksheetDto buildCard(UUID schoolId, Student student, AcademicYear year, String term) {
        return assemble(contextForStudent(schoolId, student, year, term, !isLearner(SecurityUtils.currentPrincipal())),
                student);
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
        Map<UUID, Marksheet> records = marksheetRepository
                .findBySchoolIdAndAcademicYearIdAndSectionIdAndExamTerm(schoolId, year.getId(), section.getId(), term)
                .stream()
                .collect(Collectors.toMap(Marksheet::getStudentId, Function.identity(), (a, b) -> a));
        GradingScheme scheme = resolveScheme(schoolId, year.getId(), klass.getId());
        List<GradeBoundary> boundaries = scheme == null
                ? List.of()
                : boundaryRepository.findBySchemeIdAndSchoolIdOrderBySortOrderAsc(scheme.getId(), schoolId);
        School school = schoolRepository.findById(schoolId).orElse(null);
        SectionContext ctx = new SectionContext();
        ctx.schoolId = schoolId;
        ctx.year = year;
        ctx.klass = klass;
        ctx.section = section;
        ctx.term = term;
        ctx.students = students;
        ctx.enrollments = enrollments;
        ctx.entries = entries;
        ctx.subjects = subjects;
        ctx.marksByStudent = marksByStudent;
        ctx.records = records;
        ctx.scheme = scheme;
        ctx.boundaries = boundaries;
        ctx.school = school;
        ctx.ranks = computeLiveRanks(ctx);
        return ctx;
    }

    private MarksheetDto assemble(SectionContext ctx, Student student) {
        StudentEnrollment enrollment = ctx.enrollments.get(student.getId());
        List<ExamMark> marks = ctx.marksByStudent.getOrDefault(student.getId(), List.of());
        List<MarksheetSubjectDto> subjects = ctx.entries.stream()
                .sorted(Comparator.comparing(entry -> {
                    Subject subject = ctx.subjects.get(entry.getSubjectId());
                    return subject == null ? "" : subject.getName();
                }))
                .map(entry -> toSubjectRow(entry, marks, ctx))
                .toList();
        boolean ready = !subjects.isEmpty() && subjects.stream().allMatch(row -> row.marksObtained() != null)
                && marksSubmitted(ctx.entries, marks);
        BigDecimal obtained = subjects.stream()
                .map(row -> nz(row.marksObtained()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal max = subjects.stream()
                .map(row -> nz(row.maxMarks()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal percent = max.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : obtained.multiply(HUNDRED).divide(max, 2, RoundingMode.HALF_UP);
        GradeBoundary overallBoundary = matchingBoundary(percent, ctx.boundaries);
        String grade = overallBoundary == null ? null : overallBoundary.getLabel();
        BigDecimal gpa = averageGpa(subjects, overallBoundary);
        String result = overallResult(subjects, percent, ctx.scheme);
        Marksheet stored = ctx.records.get(student.getId());
        String status = stored == null || stored.getStatus() == null ? "DRAFT" : stored.getStatus();
        boolean published = "PUBLISHED".equals(status) || (stored != null && stored.isPublished());
        boolean locked = stored != null && stored.isLocked();
        Integer rank = stored != null && stored.getClassRank() != null
                ? stored.getClassRank()
                : ctx.ranks.get(student.getId());
        String rejection = stored == null ? null : stored.getRejectionReason();
        String serial = stored == null ? serialFor(ctx, student) : stored.getSerialNo();
        LocalDate issued = stored == null || stored.getIssuedAt() == null ? LocalDate.now() : stored.getIssuedAt();
        School school = ctx.school;
        String photo = resolvePhoto(student.getPhotoUrl());
        MarksheetDto card = new MarksheetDto(
                student.getId(),
                student.getDisplayName(),
                student.getAdmissionNo(),
                enrollment == null ? null : enrollment.getRollNumber(),
                student.getDateOfBirth(),
                genderCode(student.getGender()),
                photo,
                photo == null || photo.isBlank(),
                ctx.klass.getId(),
                ctx.klass.getName(),
                ctx.section.getId(),
                ctx.section.getName(),
                ctx.year.getId(),
                ctx.year.getName(),
                ctx.term,
                ready,
                status,
                published,
                locked,
                rank,
                rejection,
                serial,
                issued,
                school == null ? "" : school.getName(),
                school == null ? "" : blank(school.getAddress()),
                school == null ? "" : blank(school.getPhone()),
                school == null ? "" : blank(school.getEmail()),
                school == null ? "" : affiliation(school),
                obtained,
                max,
                percent,
                gpa,
                grade,
                result,
                subjects);
        if (isLearner(SecurityUtils.currentPrincipal()) && !published) {
            return redact(card);
        }
        return card;
    }

    private MarksheetSubjectDto toSubjectRow(ExamEntry entry, List<ExamMark> marks, SectionContext ctx) {
        Subject subject = ctx.subjects.get(entry.getSubjectId());
        ExamMark mark = marks.stream()
                .filter(row -> entry.getId().equals(row.getExamEntryId()))
                .findFirst()
                .orElse(null);
        BigDecimal maxTotal = nz(entry.getMaxTheory()).add(nz(entry.getMaxPractical())).add(nz(entry.getMaxAssignment()));
        BigDecimal passing = passingMarks(ctx.scheme, maxTotal);
        BigDecimal obtained = mark == null ? null : mark.getTotal();
        BigDecimal percent = mark == null ? null : mark.getPercentage();
        GradeBoundary boundary = matchingBoundary(percent, ctx.boundaries);
        String grade = mark == null ? null : (mark.getGradeLabel() != null
                ? mark.getGradeLabel()
                : (boundary == null ? null : boundary.getLabel()));
        BigDecimal gradePoint = boundary == null ? null : boundary.getGpaValue();
        String result = subjectResult(percent, ctx.scheme);
        return new MarksheetSubjectDto(
                entry.getSubjectId(),
                subject == null ? "" : blank(subject.getCode()),
                subject == null ? "" : subject.getName(),
                maxTotal,
                passing,
                obtained,
                percent,
                grade,
                gradePoint,
                result);
    }

    private static String overallResult(List<MarksheetSubjectDto> subjects, BigDecimal percent, GradingScheme scheme) {
        if (subjects == null || subjects.isEmpty()) {
            return "FAIL";
        }
        long failed = subjects.stream().filter(row -> "FAIL".equals(row.result())).count();
        long passed = subjects.stream().filter(row -> "PASS".equals(row.result())).count();
        boolean overallPass = scheme == null || percent.compareTo(scheme.getPassPercent()) >= 0;
        if (failed == 0 && overallPass) {
            return "PASS";
        }
        if (passed > 0 && failed > 0 && overallPass) {
            return "COMPARTMENT";
        }
        return "FAIL";
    }

    private static String subjectResult(BigDecimal percent, GradingScheme scheme) {
        if (percent == null) {
            return "FAIL";
        }
        if (scheme == null || percent.compareTo(scheme.getPassPercent()) >= 0) {
            return "PASS";
        }
        return "FAIL";
    }

    private static BigDecimal passingMarks(GradingScheme scheme, BigDecimal maxTotal) {
        if (scheme == null || maxTotal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return scheme.getPassPercent().multiply(maxTotal).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal averageGpa(List<MarksheetSubjectDto> subjects, GradeBoundary overall) {
        List<BigDecimal> points = subjects.stream()
                .map(MarksheetSubjectDto::gradePoint)
                .filter(value -> value != null)
                .toList();
        if (!points.isEmpty()) {
            BigDecimal sum = points.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            return sum.divide(BigDecimal.valueOf(points.size()), 2, RoundingMode.HALF_UP);
        }
        return overall == null ? null : overall.getGpaValue();
    }

    private static GradeBoundary matchingBoundary(BigDecimal percent, List<GradeBoundary> boundaries) {
        if (percent == null || boundaries == null || boundaries.isEmpty()) {
            return null;
        }
        for (GradeBoundary boundary : boundaries) {
            if (percent.compareTo(boundary.getMinPercent()) >= 0
                    && percent.compareTo(boundary.getMaxPercent()) <= 0) {
                return boundary;
            }
        }
        return boundaries.get(boundaries.size() - 1);
    }

    private GradingScheme resolveScheme(UUID schoolId, UUID academicYearId, UUID classId) {
        return schemeRepository
                .findBySchoolIdAndAcademicYearIdAndClassIdAndStatus(schoolId, academicYearId, classId, "ACTIVE")
                .or(() -> schemeRepository.findBySchoolIdAndAcademicYearIdOrderByNameAsc(schoolId, academicYearId)
                        .stream()
                        .filter(scheme -> "ACTIVE".equals(scheme.getStatus()))
                        .findFirst())
                .orElse(null);
    }

    private Marksheet newRecord(SectionContext ctx, Student student) {
        Marksheet row = new Marksheet();
        row.setSchoolId(ctx.schoolId);
        row.setAcademicYearId(ctx.year.getId());
        row.setClassId(ctx.klass.getId());
        row.setSectionId(ctx.section.getId());
        row.setStudentId(student.getId());
        row.setExamTerm(ctx.term);
        row.setSerialNo(serialFor(ctx, student));
        row.setStatus("DRAFT");
        row.setPublished(false);
        row.setLocked(false);
        return row;
    }

    private SectionContext loadActionContext(UUID academicYearId, UUID classId, UUID sectionId, String examTerm) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        AcademicYear year = resolveYear(schoolId, academicYearId);
        if (sectionId == null) {
            throw new BusinessException("marksheet.section_required");
        }
        Section section = loadSection(schoolId, classId, sectionId);
        assertStaffCanAccessSection(SecurityUtils.currentPrincipal(), schoolId, section.getId());
        return loadSectionContext(schoolId, year, section, normalizeTerm(examTerm));
    }

    private void applyPublished(Marksheet stored, SectionContext ctx, Student student, Instant now, UUID actor,
                                boolean locked, Integer rank) {
        stored.setStatus("PUBLISHED");
        stored.setPublished(true);
        stored.setRejectedAt(null);
        stored.setRejectedBy(null);
        stored.setRejectionReason(null);
        if (stored.getSerialNo() == null || stored.getSerialNo().isBlank()) {
            stored.setSerialNo(serialFor(ctx, student));
        }
        if (stored.getIssuedAt() == null) {
            stored.setIssuedAt(LocalDate.now());
        }
        stored.setPublishedAt(now);
        stored.setPublishedBy(actor);
        stored.setClassRank(rank);
        stored.setLocked(locked);
        if (locked) {
            stored.setLockedAt(now);
            stored.setLockedBy(actor);
        } else {
            stored.setLockedAt(null);
            stored.setLockedBy(null);
        }
    }

    private void ensureRanks(SectionContext ctx) {
        ctx.ranks = computeLiveRanks(ctx);
    }

    private Map<UUID, Integer> computeLiveRanks(SectionContext ctx) {
        List<ScoredStudent> scored = new ArrayList<>();
        for (Student student : ctx.students) {
            List<ExamMark> marks = ctx.marksByStudent.getOrDefault(student.getId(), List.of());
            BigDecimal obtained = BigDecimal.ZERO;
            BigDecimal max = BigDecimal.ZERO;
            for (ExamEntry entry : ctx.entries) {
                BigDecimal maxTotal = nz(entry.getMaxTheory()).add(nz(entry.getMaxPractical())).add(nz(entry.getMaxAssignment()));
                max = max.add(maxTotal);
                ExamMark mark = marks.stream()
                        .filter(row -> entry.getId().equals(row.getExamEntryId()))
                        .findFirst()
                        .orElse(null);
                if (mark != null && mark.getTotal() != null) {
                    obtained = obtained.add(mark.getTotal());
                }
            }
            BigDecimal percent = max.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : obtained.multiply(HUNDRED).divide(max, 2, RoundingMode.HALF_UP);
            scored.add(new ScoredStudent(student.getId(), percent));
        }
        scored.sort(Comparator.comparing(ScoredStudent::percent).reversed()
                .thenComparing(row -> row.id().toString()));
        Map<UUID, Integer> ranks = new LinkedHashMap<>();
        BigDecimal previous = null;
        int rank = 0;
        int index = 0;
        for (ScoredStudent row : scored) {
            index += 1;
            if (previous == null || row.percent().compareTo(previous) != 0) {
                rank = index;
                previous = row.percent();
            }
            ranks.put(row.id(), rank);
        }
        return ranks;
    }

    private void resetMarksToDraft(SectionContext ctx, UUID studentId) {
        List<ExamMark> marks = ctx.marksByStudent.getOrDefault(studentId, List.of());
        for (ExamMark mark : marks) {
            if ("SUBMITTED".equals(mark.getStatus())) {
                mark.setStatus("DRAFT");
                examMarkRepository.save(mark);
            }
        }
    }

    private static boolean marksSubmitted(List<ExamEntry> entries, List<ExamMark> marks) {
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        for (ExamEntry entry : entries) {
            ExamMark mark = marks.stream()
                    .filter(row -> entry.getId().equals(row.getExamEntryId()))
                    .findFirst()
                    .orElse(null);
            if (mark == null || !"SUBMITTED".equals(mark.getStatus())) {
                return false;
            }
        }
        return true;
    }

    private static MarksheetDto redact(MarksheetDto card) {
        return new MarksheetDto(
                card.studentId(),
                card.studentName(),
                card.admissionNo(),
                card.rollNumber(),
                card.dateOfBirth(),
                card.gender(),
                card.photoUrl(),
                card.photoPlaceholder(),
                card.classId(),
                card.className(),
                card.sectionId(),
                card.sectionName(),
                card.academicYearId(),
                card.academicYearName(),
                card.examTerm(),
                card.ready(),
                card.status(),
                false,
                card.locked(),
                null,
                card.rejectionReason(),
                card.serialNo(),
                card.issueDate(),
                card.schoolName(),
                card.schoolAddress(),
                card.schoolPhone(),
                card.schoolEmail(),
                card.affiliation(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                null,
                List.of());
    }

    private static String normalizeStatusFilter(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        String value = status.trim().toUpperCase(Locale.ROOT);
        if (!List.of("DRAFT", "PENDING_APPROVAL", "PUBLISHED", "REJECTED").contains(value)) {
            throw new BusinessException("marksheet.invalid_status");
        }
        return value;
    }

    private static String serialFor(SectionContext ctx, Student student) {
        String year = ctx.year.getName() == null ? "YEAR" : ctx.year.getName().replaceAll("\\s+", "");
        String admission = student.getAdmissionNo() == null ? "NA" : student.getAdmissionNo();
        return "MS-" + year + "-" + admission + "-" + ctx.term;
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
                .orElseThrow(() -> new BusinessException("marksheet.year_required"));
    }

    private TeacherScope teacherScope(UUID schoolId, UserPrincipal principal) {
        if (principal.hasRole("ADMIN") || principal.hasRole("SUPER_ADMIN")
                || principal.permissions().contains("EXAM_MANAGE")
                || principal.permissions().contains("MARKSHEET_MANAGE")) {
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

    private static boolean matchesQuery(Student student, String needle) {
        if (needle == null || needle.isBlank()) {
            return true;
        }
        String name = student.getDisplayName() == null ? "" : student.getDisplayName().toLowerCase(Locale.ROOT);
        String admission = student.getAdmissionNo() == null ? "" : student.getAdmissionNo().toLowerCase(Locale.ROOT);
        return name.contains(needle) || admission.contains(needle);
    }

    private static boolean isLearner(UserPrincipal principal) {
        return (principal.hasRole("STUDENT") || principal.hasRole("PARENT"))
                && !principal.hasRole("ADMIN") && !principal.hasRole("SUPER_ADMIN")
                && !principal.hasRole("TEACHER");
    }

    private static boolean canManage(UserPrincipal principal) {
        return principal.permissions().contains("MARKSHEET_MANAGE")
                || principal.hasRole("ADMIN")
                || principal.hasRole("SUPER_ADMIN");
    }

    private static boolean canSubmit(UserPrincipal principal) {
        return canManage(principal)
                || principal.permissions().contains("EXAM_MARK")
                || principal.hasRole("TEACHER");
    }

    private static MarksheetStudentDto toStudentDto(MarksheetDto card) {
        return new MarksheetStudentDto(
                card.studentId(),
                card.studentName(),
                card.admissionNo(),
                card.rollNumber(),
                card.gender(),
                card.photoUrl(),
                card.ready(),
                card.status(),
                card.published(),
                card.locked(),
                card.classRank(),
                card.rejectionReason(),
                card.percentage(),
                card.gpa(),
                card.overallGrade(),
                card.result());
    }

    private static String genderCode(Gender gender) {
        return gender == null ? "OTHER" : gender.name();
    }

    private static String affiliation(School school) {
        String code = school.getCode() == null ? "" : school.getCode();
        if (code.isBlank()) {
            return "Recognised school";
        }
        return "Affiliation / School code: " + code;
    }

    private static String blank(String value) {
        return value == null ? "" : value;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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
        Map<UUID, Subject> subjects;
        Map<UUID, List<ExamMark>> marksByStudent;
        Map<UUID, Marksheet> records;
        Map<UUID, Integer> ranks = Map.of();
        GradingScheme scheme;
        List<GradeBoundary> boundaries;
        School school;
    }

    private record ScoredStudent(UUID id, BigDecimal percent) {
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
