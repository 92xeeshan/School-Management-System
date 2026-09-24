package com.schoolms.exam;

import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.ClassSubject;
import com.schoolms.academics.ClassSubjectRepository;
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
import com.schoolms.academics.TeacherSubject;
import com.schoolms.academics.TeacherSubjectRepository;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.exam.dto.ExamMarkRowDto;
import com.schoolms.exam.dto.GradeBoundaryView;
import com.schoolms.exam.dto.MarksGridDto;
import com.schoolms.exam.dto.MarksImportResult;
import com.schoolms.exam.dto.MarksLockRequest;
import com.schoolms.exam.dto.MarksOptionsDto;
import com.schoolms.exam.dto.MarksSaveRequest;
import com.schoolms.security.SecurityUtils;
import com.schoolms.security.UserPrincipal;
import com.schoolms.student.Student;
import com.schoolms.student.StudentRepository;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamMarksService {

    static final List<String> EXAM_TERMS = List.of("QUIZ", "UNIT", "MIDTERM", "TERM", "FINAL", "CONTINUOUS");
    private static final Set<String> ATTENDANCE = Set.of("PRESENT", "ABSENT", "EXCUSED");
    private static final String[] TEMPLATE_HEADERS = {
            "Admission No", "Roll", "Student Name", "Theory", "Practical", "Assignment", "Attendance", "Remarks"
    };

    private final ExamEntryRepository examEntryRepository;
    private final ExamMarkRepository examMarkRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final GradingSchemeRepository schemeRepository;
    private final GradeBoundaryRepository boundaryRepository;
    private final TeacherProfileRepository teacherRepository;
    private final TeacherSectionRepository teacherSectionRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final MarksheetService marksheetService;

    @Transactional(readOnly = true)
    public MarksOptionsDto options() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        TeacherScope scope = teacherScope(schoolId, principal);

        List<MarksOptionsDto.YearOption> years = academicYearRepository
                .findBySchoolIdOrderByStartDateDesc(schoolId).stream()
                .map(y -> new MarksOptionsDto.YearOption(y.getId(), y.getName(), y.isCurrent()))
                .toList();

        Map<UUID, List<Section>> sectionsByClass = sectionRepository.findBySchoolIdOrderByNameAsc(schoolId)
                .stream()
                .filter(section -> scope.allowsSection(section.getId()))
                .collect(Collectors.groupingBy(Section::getClassId));
        Map<UUID, List<ClassSubject>> linksByClass = classSubjectRepository.findBySchoolId(schoolId)
                .stream()
                .collect(Collectors.groupingBy(ClassSubject::getClassId));
        Map<UUID, Subject> subjects = subjectRepository.findBySchoolIdOrderByNameAsc(schoolId)
                .stream().collect(Collectors.toMap(Subject::getId, Function.identity()));

        List<MarksOptionsDto.ClassOption> classes = classRepository.findBySchoolIdOrderBySortOrderAsc(schoolId)
                .stream()
                .map(klass -> {
                    List<MarksOptionsDto.SectionOption> sections = sectionsByClass
                            .getOrDefault(klass.getId(), List.of()).stream()
                            .map(s -> new MarksOptionsDto.SectionOption(s.getId(), s.getName()))
                            .toList();
                    List<MarksOptionsDto.SubjectOption> subjectOptions = linksByClass
                            .getOrDefault(klass.getId(), List.of()).stream()
                            .map(ClassSubject::getSubjectId)
                            .distinct()
                            .filter(scope::allowsSubject)
                            .map(subjects::get)
                            .filter(subject -> subject != null)
                            .map(subject -> new MarksOptionsDto.SubjectOption(
                                    subject.getId(), subject.getName(), subject.isPractical()))
                            .toList();
                    return new MarksOptionsDto.ClassOption(klass.getId(), klass.getName(), sections, subjectOptions);
                })
                .filter(option -> !option.sections().isEmpty() && !option.subjects().isEmpty())
                .toList();

        return new MarksOptionsDto(years, classes, EXAM_TERMS);
    }

    @Transactional
    public MarksGridDto grid(UUID academicYearId, UUID classId, UUID sectionId, UUID subjectId, String examTerm) {
        Context ctx = loadContext(academicYearId, classId, sectionId, subjectId, examTerm, true);
        return toGrid(ctx);
    }

    @Transactional
    public MarksGridDto save(MarksSaveRequest request) {
        Context ctx = loadContext(request.academicYearId(), request.classId(), request.sectionId(),
                request.subjectId(), request.examTerm(), true);
        assertEditable(ctx);
        if (request.rows() == null || request.rows().isEmpty()) {
            throw new BusinessException("marks.rows_required");
        }
        Map<UUID, Student> roster = ctx.students.stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));
        String status = request.submit() ? "SUBMITTED" : "DRAFT";
        for (MarksSaveRequest.MarkRow row : request.rows()) {
            Student student = roster.get(row.studentId());
            if (student == null) {
                throw new ResourceNotFoundException("student.not_found", row.studentId());
            }
            upsertMark(ctx, student.getId(), row.theory(), row.practical(), row.assignment(),
                    row.attendanceStatus(), row.remarks(), status, true);
        }
        return toGrid(reload(ctx));
    }

    @Transactional
    public MarksGridDto lock(MarksLockRequest request) {
        if (!canManage()) {
            throw new BusinessException("auth.access_denied");
        }
        Context ctx = loadContext(request.academicYearId(), request.classId(), request.sectionId(),
                request.subjectId(), request.examTerm(), true);
        ExamEntry entry = ctx.entry;
        entry.setEntryDeadline(request.entryDeadline());
        entry.setLocked(request.locked());
        if (request.locked()) {
            entry.setManualUnlock(false);
            entry.setLockedAt(Instant.now());
            entry.setLockedBy(SecurityUtils.currentUserId());
        } else {
            entry.setManualUnlock(true);
            entry.setLockedAt(null);
            entry.setLockedBy(null);
        }
        examEntryRepository.save(entry);
        return toGrid(reload(ctx));
    }

    @Transactional
    public byte[] exportTemplate(UUID academicYearId, UUID classId, UUID sectionId, UUID subjectId,
                                 String examTerm, boolean excel) {
        Context ctx = loadContext(academicYearId, classId, sectionId, subjectId, examTerm, true);
        try {
            return excel ? writeExcel(ctx) : writeCsv(ctx);
        } catch (IOException e) {
            throw new BusinessException("marks.export_failed");
        }
    }

    public String templateFilename(boolean excel) {
        return excel ? "marks-template.xlsx" : "marks-template.csv";
    }

    public String templateContentType(boolean excel) {
        return excel
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "text/csv";
    }

    @Transactional
    public MarksImportResult importFile(UUID academicYearId, UUID classId, UUID sectionId, UUID subjectId,
                                        String examTerm, MultipartFile file) {
        Context ctx = loadContext(academicYearId, classId, sectionId, subjectId, examTerm, true);
        assertEditable(ctx);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("marks.import_failed");
        }
        List<String[]> dataRows;
        try {
            dataRows = parseUpload(file);
        } catch (IOException e) {
            throw new BusinessException("marks.import_failed");
        }
        Map<String, Student> byAdmission = ctx.students.stream()
                .collect(Collectors.toMap(s -> s.getAdmissionNo().toLowerCase(Locale.ROOT), Function.identity(),
                        (a, b) -> a));
        List<String> errors = new ArrayList<>();
        int total = 0;
        int succeeded = 0;
        int line = 1;
        for (String[] raw : dataRows) {
            line++;
            if (isBlankRow(raw)) {
                continue;
            }
            total++;
            try {
                importRow(ctx, raw, byAdmission);
                succeeded++;
            } catch (BusinessException e) {
                errors.add("Row " + line + ": " + e.getCode());
            } catch (Exception e) {
                errors.add("Row " + line + ": marks.import_failed");
            }
        }
        return new MarksImportResult(total, succeeded, errors, toGrid(reload(ctx)));
    }

    public String contentDisposition(String filename) {
        return "attachment; filename=\"" + filename + "\"";
    }

    public HttpHeaders downloadHeaders(boolean excel) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(templateFilename(excel)));
        headers.add(HttpHeaders.CONTENT_TYPE, templateContentType(excel));
        return headers;
    }

    private void importRow(Context ctx, String[] raw, Map<String, Student> byAdmission) {
        String admission = cell(raw, 0);
        if (admission.isBlank()) {
            throw new BusinessException("marks.unknown_student");
        }
        Student student = byAdmission.get(admission.toLowerCase(Locale.ROOT));
        if (student == null) {
            throw new BusinessException("marks.unknown_student");
        }
        BigDecimal theory = parseMark(cell(raw, 3));
        BigDecimal practical = parseMark(cell(raw, 4));
        BigDecimal assignment = parseMark(cell(raw, 5));
        String attendance = cell(raw, 6);
        String remarks = cell(raw, 7);
        upsertMark(ctx, student.getId(), theory, practical, assignment, attendance, remarks, "DRAFT", true);
    }

    private void upsertMark(Context ctx, UUID studentId, BigDecimal theory, BigDecimal practical,
                            BigDecimal assignment, String attendance, String remarks,
                            String status, boolean rejectInvalid) {
        validateComponent(theory, ctx.entry.getMaxTheory(), rejectInvalid, "marks.theory_exceeds");
        validateComponent(practical, ctx.entry.getMaxPractical(), rejectInvalid, "marks.practical_exceeds");
        validateComponent(assignment, ctx.entry.getMaxAssignment(), rejectInvalid, "marks.assignment_exceeds");
        String attendanceStatus = normalizeAttendance(attendance);
        ExamMark mark = examMarkRepository.findByExamEntryIdAndStudentId(ctx.entry.getId(), studentId)
                .orElseGet(() -> {
                    ExamMark created = new ExamMark();
                    created.setSchoolId(ctx.schoolId);
                    created.setExamEntryId(ctx.entry.getId());
                    created.setStudentId(studentId);
                    return created;
                });
        mark.setTheory(theory);
        mark.setPractical(practical);
        mark.setAssignment(assignment);
        mark.setAttendanceStatus(attendanceStatus);
        mark.setRemarks(blankToNull(remarks));
        applyComputed(mark, ctx);
        mark.setStatus(status);
        examMarkRepository.save(mark);
    }

    private void applyComputed(ExamMark mark, Context ctx) {
        BigDecimal total = nz(mark.getTheory()).add(nz(mark.getPractical())).add(nz(mark.getAssignment()));
        BigDecimal maxTotal = maxTotal(ctx.entry);
        BigDecimal percent = maxTotal.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : total.multiply(new BigDecimal("100")).divide(maxTotal, 2, RoundingMode.HALF_UP);
        mark.setTotal(total.setScale(2, RoundingMode.HALF_UP));
        mark.setPercentage(percent);
        mark.setGradeLabel(letterGrade(percent, ctx.boundaries));
    }

    private String letterGrade(BigDecimal percent, List<GradeBoundary> boundaries) {
        if (boundaries == null || boundaries.isEmpty()) {
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

    private void validateComponent(BigDecimal value, BigDecimal max, boolean rejectInvalid, String code) {
        if (value == null) {
            return;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(max) > 0) {
            if (rejectInvalid) {
                throw new BusinessException(code);
            }
        }
    }

    private MarksGridDto toGrid(Context ctx) {
        Map<UUID, ExamMark> existing = examMarkRepository
                .findByExamEntryIdAndSchoolId(ctx.entry.getId(), ctx.schoolId).stream()
                .collect(Collectors.toMap(ExamMark::getStudentId, Function.identity()));
        Map<UUID, Integer> rolls = rollIndex(ctx);
        List<ExamMarkRowDto> rows = ctx.students.stream()
                .sorted(Comparator.comparing((Student s) -> Optional.ofNullable(rolls.get(s.getId())).orElse(0))
                        .thenComparing(Student::getDisplayName))
                .map(student -> toRow(student, existing.get(student.getId()), rolls.get(student.getId()), ctx))
                .toList();
        boolean lockedNow = isEffectivelyLocked(ctx.entry);
        return new MarksGridDto(
                ctx.entry.getId(),
                ctx.year.getId(),
                ctx.year.getName(),
                ctx.schoolClass.getId(),
                ctx.schoolClass.getName(),
                ctx.section.getId(),
                ctx.section.getName(),
                ctx.subject.getId(),
                ctx.subject.getName(),
                ctx.subject.isPractical() || ctx.entry.getMaxPractical().compareTo(BigDecimal.ZERO) > 0,
                ctx.entry.getExamTerm(),
                ctx.entry.getMaxTheory(),
                ctx.entry.getMaxPractical(),
                ctx.entry.getMaxAssignment(),
                maxTotal(ctx.entry),
                ctx.scheme == null ? null : ctx.scheme.getPassMarks(),
                ctx.scheme == null ? "LETTER" : ctx.scheme.getScaleType(),
                ctx.scheme == null ? null : ctx.scheme.getName(),
                ctx.entry.getEntryDeadline(),
                lockedNow,
                !lockedNow && canMark(),
                canManage(),
                ctx.boundaries.stream()
                        .map(b -> new GradeBoundaryView(b.getLabel(), b.getMinPercent(), b.getMaxPercent()))
                        .toList(),
                rows);
    }

    private ExamMarkRowDto toRow(Student student, ExamMark mark, Integer roll, Context ctx) {
        BigDecimal theory = mark == null ? null : mark.getTheory();
        BigDecimal practical = mark == null ? null : mark.getPractical();
        BigDecimal assignment = mark == null ? null : mark.getAssignment();
        BigDecimal total = mark == null ? null : mark.getTotal();
        BigDecimal percent = mark == null ? null : mark.getPercentage();
        return new ExamMarkRowDto(
                student.getId(),
                student.getAdmissionNo(),
                student.getDisplayName(),
                roll,
                theory,
                practical,
                assignment,
                mark == null ? "PRESENT" : mark.getAttendanceStatus(),
                mark == null ? null : mark.getRemarks(),
                total,
                percent,
                mark == null ? null : mark.getGradeLabel(),
                mark == null ? "DRAFT" : mark.getStatus(),
                exceeds(theory, ctx.entry.getMaxTheory()),
                exceeds(practical, ctx.entry.getMaxPractical()),
                exceeds(assignment, ctx.entry.getMaxAssignment()));
    }

    private Context loadContext(UUID academicYearId, UUID classId, UUID sectionId, UUID subjectId,
                                String examTerm, boolean create) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        TeacherScope scope = teacherScope(schoolId, principal);
        String term = normalizeTerm(examTerm);
        AcademicYear year = academicYearRepository.findByIdAndSchoolId(academicYearId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_year", academicYearId));
        SchoolClass schoolClass = classRepository.findByIdAndSchoolId(classId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("class", classId));
        Section section = sectionRepository.findByIdAndSchoolId(sectionId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("section", sectionId));
        if (!classId.equals(section.getClassId())) {
            throw new BusinessException("validation.invalid");
        }
        Subject subject = subjectRepository.findByIdAndSchoolId(subjectId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("subject", subjectId));
        if (!classSubjectRepository.existsByClassIdAndSubjectId(classId, subjectId)) {
            throw new BusinessException("marks.subject_not_in_class");
        }
        if (!scope.allowsSection(sectionId) || !scope.allowsSubject(subjectId)) {
            throw new BusinessException("auth.access_denied");
        }
        ExamEntry entry = examEntryRepository
                .findBySchoolIdAndAcademicYearIdAndSectionIdAndSubjectIdAndExamTerm(
                        schoolId, academicYearId, sectionId, subjectId, term)
                .orElseGet(() -> {
                    if (!create) {
                        throw new ResourceNotFoundException("exam_entry", subjectId);
                    }
                    ExamEntry created = new ExamEntry();
                    created.setSchoolId(schoolId);
                    created.setAcademicYearId(academicYearId);
                    created.setClassId(classId);
                    created.setSectionId(sectionId);
                    created.setSubjectId(subjectId);
                    created.setExamTerm(term);
                    applyDefaultMaxima(created, schoolClass, year, subject);
                    return examEntryRepository.save(created);
                });
        GradingScheme scheme = schemeRepository
                .findBySchoolIdAndAcademicYearIdAndClassIdAndStatus(schoolId, academicYearId, classId, "ACTIVE")
                .orElse(null);
        List<GradeBoundary> boundaries = scheme == null
                ? List.of()
                : boundaryRepository.findBySchemeIdAndSchoolIdOrderBySortOrderAsc(scheme.getId(), schoolId);
        List<Student> students = studentRepository.findBySectionAndYear(schoolId, sectionId, academicYearId);
        List<StudentEnrollment> enrollments = enrollmentRepository
                .findBySectionIdAndAcademicYearId(sectionId, academicYearId);
        return new Context(schoolId, year, schoolClass, section, subject, entry, scheme, boundaries, students,
                enrollments);
    }

    private Context reload(Context ctx) {
        ExamEntry entry = examEntryRepository.findByIdAndSchoolId(ctx.entry.getId(), ctx.schoolId)
                .orElse(ctx.entry);
        return new Context(ctx.schoolId, ctx.year, ctx.schoolClass, ctx.section, ctx.subject, entry,
                ctx.scheme, ctx.boundaries, ctx.students, ctx.enrollments);
    }

    private void applyDefaultMaxima(ExamEntry entry, SchoolClass schoolClass, AcademicYear year, Subject subject) {
        GradingScheme scheme = schemeRepository
                .findBySchoolIdAndAcademicYearIdAndClassIdAndStatus(
                        entry.getSchoolId(), year.getId(), schoolClass.getId(), "ACTIVE")
                .orElse(null);
        BigDecimal maxTotal = scheme == null ? new BigDecimal("100") : scheme.getMaxMarks();
        if (subject.isPractical()) {
            entry.setMaxTheory(percentOf(maxTotal, 60));
            entry.setMaxPractical(percentOf(maxTotal, 20));
            entry.setMaxAssignment(percentOf(maxTotal, 20));
        } else {
            entry.setMaxTheory(percentOf(maxTotal, 80));
            entry.setMaxPractical(BigDecimal.ZERO);
            entry.setMaxAssignment(percentOf(maxTotal, 20));
        }
    }

    private void assertEditable(Context ctx) {
        if (!canMark()) {
            throw new BusinessException("auth.access_denied");
        }
        if (isEffectivelyLocked(ctx.entry)) {
            throw new BusinessException("marks.locked");
        }
        if (marksheetService.sectionTermLocked(ctx.entry.getSchoolId(), ctx.entry.getAcademicYearId(),
                ctx.entry.getSectionId(), ctx.entry.getExamTerm())) {
            throw new BusinessException("marksheet.locked");
        }
    }

    private boolean isEffectivelyLocked(ExamEntry entry) {
        if (entry.isLocked()) {
            return true;
        }
        if (entry.isManualUnlock()) {
            return false;
        }
        return entry.getEntryDeadline() != null && LocalDate.now().isAfter(entry.getEntryDeadline());
    }

    private TeacherScope teacherScope(UUID schoolId, UserPrincipal principal) {
        if (principal.hasRole("ADMIN") || principal.hasRole("SUPER_ADMIN") || canManage()) {
            return TeacherScope.all();
        }
        TeacherProfile teacher = teacherRepository.findBySchoolIdAndUserId(schoolId, principal.id()).orElse(null);
        if (teacher == null) {
            return TeacherScope.all();
        }
        Set<UUID> sections = teacherSectionRepository.findByTeacherIdAndSchoolId(teacher.getId(), schoolId)
                .stream().map(TeacherSection::getSectionId).collect(Collectors.toSet());
        Set<UUID> subjects = teacherSubjectRepository.findByTeacherIdAndSchoolId(teacher.getId(), schoolId)
                .stream().map(TeacherSubject::getSubjectId).collect(Collectors.toSet());
        subjects.addAll(classSubjectRepository.findBySchoolId(schoolId).stream()
                .filter(link -> teacher.getId().equals(link.getTeacherId()))
                .map(ClassSubject::getSubjectId)
                .collect(Collectors.toSet()));
        if (sections.isEmpty() && subjects.isEmpty()) {
            return TeacherScope.all();
        }
        return new TeacherScope(sections, subjects);
    }

    private boolean canMark() {
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        return principal.permissions().contains("EXAM_MARK") || principal.permissions().contains("EXAM_MANAGE");
    }

    private boolean canManage() {
        return SecurityUtils.currentPrincipal().permissions().contains("EXAM_MANAGE");
    }

    private String normalizeTerm(String examTerm) {
        if (examTerm == null) {
            throw new BusinessException("validation.invalid");
        }
        String term = examTerm.trim().toUpperCase(Locale.ROOT);
        if (!EXAM_TERMS.contains(term)) {
            throw new BusinessException("validation.invalid");
        }
        return term;
    }

    private String normalizeAttendance(String value) {
        if (value == null || value.isBlank()) {
            return "PRESENT";
        }
        String status = value.trim().toUpperCase(Locale.ROOT);
        if (!ATTENDANCE.contains(status)) {
            throw new BusinessException("marks.invalid_attendance");
        }
        return status;
    }

    private List<String[]> parseUpload(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
            return parseExcel(file);
        }
        return parseCsv(file);
    }

    private List<String[]> parseCsv(MultipartFile file) throws IOException {
        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)).build()) {
            List<String[]> rows = new ArrayList<>();
            String[] row;
            boolean header = true;
            try {
                while ((row = reader.readNext()) != null) {
                    if (header) {
                        header = false;
                        continue;
                    }
                    rows.add(row);
                }
            } catch (CsvValidationException e) {
                throw new BusinessException("marks.import_failed");
            }
            return rows;
        }
    }

    private List<String[]> parseExcel(MultipartFile file) throws IOException {
        DataFormatter formatter = new DataFormatter();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                return List.of();
            }
            List<String[]> rows = new ArrayList<>();
            boolean header = true;
            for (Row row : sheet) {
                if (header) {
                    header = false;
                    continue;
                }
                String[] cells = new String[8];
                for (int i = 0; i < 8; i++) {
                    Cell cell = row.getCell(i);
                    cells[i] = cell == null ? "" : formatter.formatCellValue(cell).trim();
                }
                rows.add(cells);
            }
            return rows;
        }
    }

    private byte[] writeCsv(Context ctx) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writer.writeNext(TEMPLATE_HEADERS);
            Map<UUID, ExamMark> existing = examMarkRepository
                    .findByExamEntryIdAndSchoolId(ctx.entry.getId(), ctx.schoolId).stream()
                    .collect(Collectors.toMap(ExamMark::getStudentId, Function.identity()));
            Map<UUID, Integer> rolls = rollIndex(ctx);
            for (Student student : ctx.students) {
                ExamMark mark = existing.get(student.getId());
                writer.writeNext(new String[]{
                        student.getAdmissionNo(),
                        String.valueOf(Optional.ofNullable(rolls.get(student.getId())).orElse(0)),
                        student.getDisplayName(),
                        markValue(mark == null ? null : mark.getTheory()),
                        markValue(mark == null ? null : mark.getPractical()),
                        markValue(mark == null ? null : mark.getAssignment()),
                        mark == null ? "PRESENT" : mark.getAttendanceStatus(),
                        mark == null || mark.getRemarks() == null ? "" : mark.getRemarks()
                });
            }
        }
        return out.toByteArray();
    }

    private byte[] writeExcel(Context ctx) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Marks");
            Row header = sheet.createRow(0);
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                header.createCell(i, CellType.STRING).setCellValue(TEMPLATE_HEADERS[i]);
            }
            Map<UUID, ExamMark> existing = examMarkRepository
                    .findByExamEntryIdAndSchoolId(ctx.entry.getId(), ctx.schoolId).stream()
                    .collect(Collectors.toMap(ExamMark::getStudentId, Function.identity()));
            Map<UUID, Integer> rolls = rollIndex(ctx);
            int r = 1;
            for (Student student : ctx.students) {
                ExamMark mark = existing.get(student.getId());
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(student.getAdmissionNo());
                row.createCell(1).setCellValue(Optional.ofNullable(rolls.get(student.getId())).orElse(0));
                row.createCell(2).setCellValue(student.getDisplayName());
                setNumeric(row.createCell(3), mark == null ? null : mark.getTheory());
                setNumeric(row.createCell(4), mark == null ? null : mark.getPractical());
                setNumeric(row.createCell(5), mark == null ? null : mark.getAssignment());
                row.createCell(6).setCellValue(mark == null ? "PRESENT" : mark.getAttendanceStatus());
                row.createCell(7).setCellValue(mark == null || mark.getRemarks() == null ? "" : mark.getRemarks());
            }
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void setNumeric(Cell cell, BigDecimal value) {
        if (value == null) {
            cell.setBlank();
            return;
        }
        cell.setCellValue(value.doubleValue());
    }

    private static boolean exceeds(BigDecimal value, BigDecimal max) {
        return value != null && (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(max) > 0);
    }

    private static Map<UUID, Integer> rollIndex(Context ctx) {
        Map<UUID, Integer> rolls = new HashMap<>();
        for (StudentEnrollment enrollment : ctx.enrollments) {
            rolls.putIfAbsent(enrollment.getStudentId(),
                    enrollment.getRollNumber() == null ? 0 : enrollment.getRollNumber());
        }
        return rolls;
    }

    private static BigDecimal maxTotal(ExamEntry entry) {
        return nz(entry.getMaxTheory()).add(nz(entry.getMaxPractical())).add(nz(entry.getMaxAssignment()));
    }

    private static BigDecimal percentOf(BigDecimal total, int percent) {
        return total.multiply(BigDecimal.valueOf(percent))
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal parseMark(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("validation.invalid");
        }
    }

    private static String cell(String[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return "";
        }
        return row[index].trim();
    }

    private static boolean isBlankRow(String[] row) {
        if (row == null) {
            return true;
        }
        for (String cell : row) {
            if (cell != null && !cell.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String markValue(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record Context(
            UUID schoolId,
            AcademicYear year,
            SchoolClass schoolClass,
            Section section,
            Subject subject,
            ExamEntry entry,
            GradingScheme scheme,
            List<GradeBoundary> boundaries,
            List<Student> students,
            List<StudentEnrollment> enrollments
    ) {
    }

    private record TeacherScope(Set<UUID> sections, Set<UUID> subjects) {
        static TeacherScope all() {
            return new TeacherScope(null, null);
        }

        boolean allowsSection(UUID sectionId) {
            return sections == null || sections.isEmpty() || sections.contains(sectionId);
        }

        boolean allowsSubject(UUID subjectId) {
            return subjects == null || subjects.isEmpty() || subjects.contains(subjectId);
        }
    }
}
