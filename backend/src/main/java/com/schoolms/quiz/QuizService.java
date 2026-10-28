package com.schoolms.quiz;

import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.ClassSubject;
import com.schoolms.academics.ClassSubjectRepository;
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
import com.schoolms.common.api.PageRequests;
import com.schoolms.common.api.PagedResponse;
import com.schoolms.common.enums.QuizAttemptStatus;
import com.schoolms.common.enums.QuizQuestionType;
import com.schoolms.common.enums.QuizStatus;
import com.schoolms.common.exception.AuthException;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.common.exception.ResourceNotFoundException;
import com.schoolms.quiz.dto.QuestionOptionDto;
import com.schoolms.quiz.dto.QuizAnswerDto;
import com.schoolms.quiz.dto.QuizAttemptDto;
import com.schoolms.quiz.dto.QuizDto;
import com.schoolms.quiz.dto.QuizGradeRequest;
import com.schoolms.quiz.dto.QuizItemDto;
import com.schoolms.quiz.dto.QuizItemRequest;
import com.schoolms.quiz.dto.QuizOfflineGridDto;
import com.schoolms.quiz.dto.QuizOfflineRowDto;
import com.schoolms.quiz.dto.QuizOfflineSaveRequest;
import com.schoolms.quiz.dto.QuizOptionsDto;
import com.schoolms.quiz.dto.QuizQuestionDto;
import com.schoolms.quiz.dto.QuizQuestionRequest;
import com.schoolms.quiz.dto.QuizRequest;
import com.schoolms.quiz.dto.QuizResultRowDto;
import com.schoolms.quiz.dto.QuizStatusRequest;
import com.schoolms.quiz.dto.QuizSubmitRequest;
import com.schoolms.quiz.dto.QuizTakeDto;
import com.schoolms.quiz.dto.QuizTakeQuestionDto;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizQuestionRepository questionRepository;
    private final QuizRepository quizRepository;
    private final QuizItemRepository itemRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizAnswerRepository answerRepository;
    private final QuizOfflineMarkRepository offlineMarkRepository;
    private final QuizPdfService pdfService;
    private final AcademicYearRepository academicYearRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final TeacherProfileRepository teacherRepository;
    private final TeacherSectionRepository teacherSectionRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final SchoolRepository schoolRepository;

    @Transactional(readOnly = true)
    public QuizOptionsDto options() {
        UUID schoolId = SecurityUtils.currentSchoolId();
        TeacherScope scope = teacherScope(schoolId, SecurityUtils.currentPrincipal());

        List<QuizOptionsDto.YearOption> years = academicYearRepository
                .findBySchoolIdOrderByStartDateDesc(schoolId).stream()
                .map(y -> new QuizOptionsDto.YearOption(y.getId(), y.getName(), y.isCurrent()))
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

        List<QuizOptionsDto.ClassOption> classes = classRepository.findBySchoolIdOrderBySortOrderAsc(schoolId)
                .stream()
                .map(klass -> {
                    List<QuizOptionsDto.SectionOption> sections = sectionsByClass
                            .getOrDefault(klass.getId(), List.of()).stream()
                            .map(s -> new QuizOptionsDto.SectionOption(s.getId(), s.getName()))
                            .toList();
                    List<QuizOptionsDto.SubjectOption> subjectOptions = linksByClass
                            .getOrDefault(klass.getId(), List.of()).stream()
                            .map(ClassSubject::getSubjectId)
                            .distinct()
                            .filter(scope::allowsSubject)
                            .map(subjects::get)
                            .filter(subject -> subject != null)
                            .map(subject -> new QuizOptionsDto.SubjectOption(subject.getId(), subject.getName()))
                            .toList();
                    if (subjectOptions.isEmpty()) {
                        subjectOptions = subjects.values().stream()
                                .filter(subject -> scope.allowsSubject(subject.getId()))
                                .map(subject -> new QuizOptionsDto.SubjectOption(subject.getId(), subject.getName()))
                                .toList();
                    }
                    return new QuizOptionsDto.ClassOption(klass.getId(), klass.getName(), sections, subjectOptions);
                })
                .filter(option -> !option.sections().isEmpty())
                .toList();

        return new QuizOptionsDto(years, classes,
                List.of("MCQ", "TRUE_FALSE", "SHORT_ANSWER"),
                List.of("DRAFT", "PUBLISHED", "CLOSED", "ARCHIVED"));
    }

    @Transactional(readOnly = true)
    public List<QuizQuestionDto> listQuestions(UUID subjectId, UUID classId, String questionType) {
        return listQuestionDtos(subjectId, classId, questionType);
    }

    @Transactional(readOnly = true)
    public PagedResponse<QuizQuestionDto> listQuestions(UUID subjectId, UUID classId, String questionType,
                                                        String query, int page, int size, String sort, String direction) {
        List<QuizQuestionDto> questions = listQuestionDtos(subjectId, classId, questionType);
        String normalized = PageRequests.normalizeQuery(query);
        if (normalized != null) {
            String needle = normalized.toLowerCase(Locale.ROOT);
            questions = questions.stream()
                    .filter(q -> containsIgnoreCase(q.prompt(), needle)
                            || containsIgnoreCase(q.subjectName(), needle)
                            || containsIgnoreCase(q.className(), needle))
                    .toList();
        }
        questions = sortQuestions(questions, sort, direction);
        return PageRequests.fromList(questions, page, size);
    }

    private List<QuizQuestionDto> listQuestionDtos(UUID subjectId, UUID classId, String questionType) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        TeacherScope scope = teacherScope(schoolId, SecurityUtils.currentPrincipal());
        QuizQuestionType type = parseQuestionType(questionType, false);
        List<QuizQuestion> questions = questionRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId).stream()
                .filter(q -> subjectId == null || subjectId.equals(q.getSubjectId()))
                .filter(q -> classId == null || classId.equals(q.getClassId()))
                .filter(q -> type == null || q.getQuestionType() == type)
                .filter(q -> q.getSubjectId() == null || scope.allowsSubject(q.getSubjectId()))
                .toList();
        return toQuestionDtos(schoolId, questions, true);
    }

    @Transactional
    public QuizQuestionDto createQuestion(QuizQuestionRequest request) {
        assertCanManage();
        UUID schoolId = SecurityUtils.currentSchoolId();
        TeacherScope scope = teacherScope(schoolId, SecurityUtils.currentPrincipal());
        QuizQuestion question = new QuizQuestion();
        question.setSchoolId(schoolId);
        question.setCreatedByUserId(SecurityUtils.currentUserId());
        applyQuestion(question, request, schoolId, scope);
        return toQuestionDto(questionRepository.save(question), names(schoolId), true);
    }

    @Transactional
    public QuizQuestionDto updateQuestion(UUID id, QuizQuestionRequest request) {
        assertCanManage();
        UUID schoolId = SecurityUtils.currentSchoolId();
        TeacherScope scope = teacherScope(schoolId, SecurityUtils.currentPrincipal());
        QuizQuestion question = requireQuestion(id, schoolId);
        if (question.getSubjectId() != null) {
            assertSubjectAllowed(scope, question.getSubjectId());
        }
        applyQuestion(question, request, schoolId, scope);
        return toQuestionDto(questionRepository.save(question), names(schoolId), true);
    }

    @Transactional
    public void deleteQuestion(UUID id) {
        assertCanManage();
        UUID schoolId = SecurityUtils.currentSchoolId();
        QuizQuestion question = requireQuestion(id, schoolId);
        TeacherScope scope = teacherScope(schoolId, SecurityUtils.currentPrincipal());
        if (question.getSubjectId() != null) {
            assertSubjectAllowed(scope, question.getSubjectId());
        }
        if (itemRepository.existsByQuestionId(id)) {
            throw new BusinessException("quiz.question_in_use");
        }
        questionRepository.delete(question);
    }

    @Transactional(readOnly = true)
    public List<QuizDto> listQuizzes(UUID academicYearId, UUID classId, UUID sectionId, UUID subjectId,
                                     String status, UUID studentId) {
        return listQuizDtos(academicYearId, classId, sectionId, subjectId, status, studentId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<QuizDto> listQuizzes(UUID academicYearId, UUID classId, UUID sectionId, UUID subjectId,
                                              String status, UUID studentId, String query, int page, int size,
                                              String sort, String direction) {
        List<QuizDto> quizzes = listQuizDtos(academicYearId, classId, sectionId, subjectId, status, studentId);
        String normalized = PageRequests.normalizeQuery(query);
        if (normalized != null) {
            String needle = normalized.toLowerCase(Locale.ROOT);
            quizzes = quizzes.stream()
                    .filter(q -> containsIgnoreCase(q.title(), needle)
                            || containsIgnoreCase(q.className(), needle)
                            || containsIgnoreCase(q.subjectName(), needle)
                            || containsIgnoreCase(q.sectionName(), needle))
                    .toList();
        }
        quizzes = sortQuizzes(quizzes, sort, direction);
        return PageRequests.fromList(quizzes, page, size);
    }

    private List<QuizDto> listQuizDtos(UUID academicYearId, UUID classId, UUID sectionId, UUID subjectId,
                                       String status, UUID studentId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        TeacherScope scope = teacherScope(schoolId, principal);
        QuizStatus quizStatus = parseStatus(status, false);
        Student learner = resolveLearner(principal, studentId);
        StudentEnrollment enrollment = learner == null ? null : currentEnrollment(schoolId, learner.getId());

        List<Quiz> quizzes = quizRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId).stream()
                .filter(q -> academicYearId == null || academicYearId.equals(q.getAcademicYearId()))
                .filter(q -> classId == null || classId.equals(q.getClassId()))
                .filter(q -> sectionId == null || sectionId.equals(q.getSectionId()) || q.getSectionId() == null)
                .filter(q -> subjectId == null || subjectId.equals(q.getSubjectId()))
                .filter(q -> quizStatus == null || q.getStatus() == quizStatus)
                .filter(q -> scope.allowsSection(q.getSectionId()) && scope.allowsSubject(q.getSubjectId()))
                .filter(q -> learner == null || (enrollment != null && visibleToLearner(q, enrollment)))
                .toList();
        return toQuizDtos(schoolId, quizzes, false);
    }

    @Transactional(readOnly = true)
    public QuizDto getQuiz(UUID id) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        Quiz quiz = requireQuiz(id, schoolId);
        assertCanViewQuiz(principal, schoolId, quiz);
        return toQuizDto(quiz, names(schoolId), itemsFor(schoolId, quiz.getId()),
                attemptRepository.findBySchoolIdAndQuizIdOrderByAttemptNoAsc(schoolId, quiz.getId()).size(),
                canManage());
    }

    @Transactional
    public QuizDto createQuiz(QuizRequest request) {
        assertCanManage();
        UUID schoolId = SecurityUtils.currentSchoolId();
        TeacherScope scope = teacherScope(schoolId, SecurityUtils.currentPrincipal());
        Quiz quiz = new Quiz();
        quiz.setSchoolId(schoolId);
        quiz.setCreatedByUserId(SecurityUtils.currentUserId());
        quiz.setStatus(QuizStatus.DRAFT);
        applyQuiz(quiz, request, schoolId, scope);
        quiz = quizRepository.save(quiz);
        replaceItems(quiz, request.items(), schoolId, scope);
        return getQuiz(quiz.getId());
    }

    @Transactional
    public QuizDto updateQuiz(UUID id, QuizRequest request) {
        assertCanManage();
        UUID schoolId = SecurityUtils.currentSchoolId();
        TeacherScope scope = teacherScope(schoolId, SecurityUtils.currentPrincipal());
        Quiz quiz = requireQuiz(id, schoolId);
        assertSubjectAllowed(scope, quiz.getSubjectId());
        if (quiz.getStatus() != QuizStatus.DRAFT) {
            throw new BusinessException("quiz.not_editable");
        }
        applyQuiz(quiz, request, schoolId, scope);
        quizRepository.save(quiz);
        replaceItems(quiz, request.items(), schoolId, scope);
        return getQuiz(quiz.getId());
    }

    @Transactional
    public QuizDto updateStatus(UUID id, QuizStatusRequest request) {
        assertCanManage();
        UUID schoolId = SecurityUtils.currentSchoolId();
        Quiz quiz = requireQuiz(id, schoolId);
        TeacherScope scope = teacherScope(schoolId, SecurityUtils.currentPrincipal());
        assertSubjectAllowed(scope, quiz.getSubjectId());
        QuizStatus next = parseStatus(request.status(), true);
        if (next == QuizStatus.PUBLISHED) {
            if (itemRepository.countByQuizId(quiz.getId()) == 0) {
                throw new BusinessException("quiz.no_questions");
            }
        }
        quiz.setStatus(next);
        quizRepository.save(quiz);
        return getQuiz(quiz.getId());
    }

    @Transactional
    public void deleteQuiz(UUID id) {
        assertCanManage();
        UUID schoolId = SecurityUtils.currentSchoolId();
        Quiz quiz = requireQuiz(id, schoolId);
        TeacherScope scope = teacherScope(schoolId, SecurityUtils.currentPrincipal());
        assertSubjectAllowed(scope, quiz.getSubjectId());
        if (quiz.getStatus() == QuizStatus.PUBLISHED) {
            throw new BusinessException("quiz.cannot_delete_published");
        }
        if (!attemptRepository.findBySchoolIdAndQuizIdOrderByAttemptNoAsc(schoolId, quiz.getId()).isEmpty()) {
            throw new BusinessException("quiz.has_attempts");
        }
        quizRepository.delete(quiz);
    }

    @Transactional
    public QuizTakeDto startAttempt(UUID quizId, UUID studentId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        Quiz quiz = requireQuiz(quizId, schoolId);
        Student student = requireAccessibleStudent(principal, schoolId, studentId, true);
        assertLearnerCanTake(quiz, student);
        Instant now = Instant.now();
        if (quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new BusinessException("quiz.not_open");
        }
        if (quiz.getStartAt() != null && now.isBefore(quiz.getStartAt())) {
            throw new BusinessException("quiz.not_started");
        }
        if (quiz.getEndAt() != null && now.isAfter(quiz.getEndAt())) {
            throw new BusinessException("quiz.closed");
        }
        QuizAttempt open = attemptRepository
                .findFirstBySchoolIdAndQuizIdAndStudentIdAndStatus(schoolId, quizId, student.getId(),
                        QuizAttemptStatus.IN_PROGRESS)
                .orElse(null);
        if (open != null) {
            if (!now.isBefore(open.getExpiresAt())) {
                finalizeAttempt(open, List.of(), true);
                open = null;
            } else {
                return toTakeDto(quiz, open, schoolId);
            }
        }
        long used = attemptRepository.countByQuizIdAndStudentId(quizId, student.getId());
        if (used >= quiz.getMaxAttempts()) {
            throw new BusinessException("quiz.attempts_exhausted");
        }
        QuizAttempt attempt = new QuizAttempt();
        attempt.setSchoolId(schoolId);
        attempt.setQuizId(quizId);
        attempt.setStudentId(student.getId());
        attempt.setAttemptNo((int) used + 1);
        attempt.setStatus(QuizAttemptStatus.IN_PROGRESS);
        attempt.setStartedAt(now);
        attempt.setExpiresAt(now.plusSeconds(quiz.getDurationMinutes() * 60L));
        attempt.setMaxScore(sumMarks(itemsFor(schoolId, quizId)));
        attempt = attemptRepository.save(attempt);
        return toTakeDto(quiz, attempt, schoolId);
    }

    @Transactional
    public QuizAttemptDto saveProgress(UUID attemptId, QuizSubmitRequest request) {
        return persistAnswers(attemptId, request, false);
    }

    @Transactional
    public QuizAttemptDto submitAttempt(UUID attemptId, QuizSubmitRequest request) {
        return persistAnswers(attemptId, request, true);
    }

    @Transactional(readOnly = true)
    public QuizAttemptDto getAttempt(UUID attemptId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        QuizAttempt attempt = requireAttempt(attemptId, schoolId);
        Quiz quiz = requireQuiz(attempt.getQuizId(), schoolId);
        assertCanViewAttempt(principal, schoolId, quiz, attempt);
        Names names = names(schoolId);
        return toAttemptDto(attempt, quiz, names, answersFor(schoolId, attempt.getId()), canManage() || quiz.isShowResults());
    }

    @Transactional
    public QuizAttemptDto gradeAttempt(UUID attemptId, QuizGradeRequest request) {
        assertCanManage();
        UUID schoolId = SecurityUtils.currentSchoolId();
        QuizAttempt attempt = requireAttempt(attemptId, schoolId);
        Quiz quiz = requireQuiz(attempt.getQuizId(), schoolId);
        TeacherScope scope = teacherScope(schoolId, SecurityUtils.currentPrincipal());
        assertSubjectAllowed(scope, quiz.getSubjectId());
        if (attempt.getStatus() == QuizAttemptStatus.IN_PROGRESS) {
            throw new BusinessException("quiz.attempt_not_submitted");
        }
        List<QuizItem> items = itemsFor(schoolId, quiz.getId());
        Map<UUID, QuizItem> itemByQuestion = items.stream()
                .collect(Collectors.toMap(QuizItem::getQuestionId, Function.identity(), (a, b) -> a));
        Map<UUID, QuizAnswer> answers = answersFor(schoolId, attempt.getId()).stream()
                .collect(Collectors.toMap(QuizAnswer::getQuestionId, Function.identity(), (a, b) -> a));
        for (QuizGradeRequest.GradeRow row : request.answers()) {
            QuizItem item = itemByQuestion.get(row.questionId());
            if (item == null) {
                throw new BusinessException("quiz.question_not_in_quiz");
            }
            QuizAnswer answer = answers.computeIfAbsent(row.questionId(), questionId -> {
                QuizAnswer created = new QuizAnswer();
                created.setSchoolId(schoolId);
                created.setAttemptId(attempt.getId());
                created.setQuestionId(questionId);
                return created;
            });
            BigDecimal awarded = row.awardedMarks() == null ? BigDecimal.ZERO : row.awardedMarks();
            if (awarded.signum() < 0 || awarded.compareTo(item.getMarks()) > 0) {
                throw new BusinessException("quiz.marks_out_of_range");
            }
            answer.setAwardedMarks(awarded);
            answer.setGraderUserId(SecurityUtils.currentUserId());
            answer.setGraderComment(blankToNull(row.graderComment()));
            answerRepository.save(answer);
        }
        scoreAttempt(attempt, items, new ArrayList<>(answers.values()));
        attempt.setStatus(QuizAttemptStatus.GRADED);
        attemptRepository.save(attempt);
        return getAttempt(attemptId);
    }

    @Transactional(readOnly = true)
    public List<QuizAttemptDto> listAttempts(UUID quizId) {
        assertCanManage();
        UUID schoolId = SecurityUtils.currentSchoolId();
        Quiz quiz = requireQuiz(quizId, schoolId);
        TeacherScope scope = teacherScope(schoolId, SecurityUtils.currentPrincipal());
        assertSubjectAllowed(scope, quiz.getSubjectId());
        Names names = names(schoolId);
        List<QuizAttempt> attempts = attemptRepository.findBySchoolIdAndQuizIdOrderByAttemptNoAsc(schoolId, quizId);
        Map<UUID, List<QuizAnswer>> answers = answersByAttempt(schoolId, attempts);
        return attempts.stream()
                .map(attempt -> toAttemptDto(attempt, quiz, names,
                        answers.getOrDefault(attempt.getId(), List.of()), true))
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizOfflineGridDto offlineGrid(UUID quizId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        Quiz quiz = requireQuiz(quizId, schoolId);
        TeacherScope scope = teacherScope(schoolId, SecurityUtils.currentPrincipal());
        assertCanViewQuiz(SecurityUtils.currentPrincipal(), schoolId, quiz);
        Names names = names(schoolId);
        List<Student> students = roster(schoolId, quiz);
        Map<UUID, StudentEnrollment> enrollments = enrollmentsFor(schoolId, quiz, students);
        Map<UUID, QuizOfflineMark> marks = offlineMarkRepository.findBySchoolIdAndQuizId(schoolId, quizId).stream()
                .collect(Collectors.toMap(QuizOfflineMark::getStudentId, Function.identity(), (a, b) -> a));
        List<QuizOfflineRowDto> rows = students.stream()
                .sorted(studentOrder(enrollments))
                .map(student -> {
                    QuizOfflineMark mark = marks.get(student.getId());
                    StudentEnrollment enrollment = enrollments.get(student.getId());
                    return new QuizOfflineRowDto(
                            student.getId(), student.getAdmissionNo(), student.getDisplayName(),
                            enrollment == null ? null : enrollment.getRollNumber(),
                            mark == null ? null : mark.getMarks(),
                            mark == null ? null : mark.getRemarks());
                })
                .toList();
        return new QuizOfflineGridDto(quiz.getId(), quiz.getTitle(), quiz.getClassId(),
                names.className(quiz.getClassId()), quiz.getSectionId(), names.sectionName(quiz.getSectionId()),
                quiz.getSubjectId(), names.subjectName(quiz.getSubjectId()),
                sumMarks(itemsFor(schoolId, quizId)), canManage() && scope.allowsSubject(quiz.getSubjectId()), rows);
    }

    @Transactional
    public QuizOfflineGridDto saveOffline(UUID quizId, QuizOfflineSaveRequest request) {
        assertCanManage();
        UUID schoolId = SecurityUtils.currentSchoolId();
        Quiz quiz = requireQuiz(quizId, schoolId);
        TeacherScope scope = teacherScope(schoolId, SecurityUtils.currentPrincipal());
        assertSubjectAllowed(scope, quiz.getSubjectId());
        BigDecimal max = sumMarks(itemsFor(schoolId, quizId));
        Set<UUID> roster = roster(schoolId, quiz).stream().map(Student::getId).collect(Collectors.toSet());
        for (QuizOfflineSaveRequest.Row row : request.rows()) {
            if (!roster.contains(row.studentId())) {
                throw new BusinessException("quiz.student_not_in_roster");
            }
            if (row.marks() != null && (row.marks().signum() < 0 || (max != null && row.marks().compareTo(max) > 0))) {
                throw new BusinessException("quiz.marks_out_of_range");
            }
            QuizOfflineMark mark = offlineMarkRepository.findByQuizIdAndStudentId(quizId, row.studentId())
                    .orElseGet(() -> {
                        QuizOfflineMark created = new QuizOfflineMark();
                        created.setSchoolId(schoolId);
                        created.setQuizId(quizId);
                        created.setStudentId(row.studentId());
                        return created;
                    });
            mark.setMarks(row.marks());
            mark.setRemarks(blankToNull(row.remarks()));
            offlineMarkRepository.save(mark);
        }
        return offlineGrid(quizId);
    }

    @Transactional(readOnly = true)
    public List<QuizResultRowDto> results(UUID quizId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        Quiz quiz = requireQuiz(quizId, schoolId);
        assertCanViewQuiz(principal, schoolId, quiz);
        Names names = names(schoolId);
        List<Student> students = roster(schoolId, quiz);
        Map<UUID, StudentEnrollment> enrollments = enrollmentsFor(schoolId, quiz, students);
        Map<UUID, QuizAttempt> bestAttempts = bestAttempts(schoolId, quizId);
        Map<UUID, QuizOfflineMark> offline = offlineMarkRepository.findBySchoolIdAndQuizId(schoolId, quizId).stream()
                .collect(Collectors.toMap(QuizOfflineMark::getStudentId, Function.identity(), (a, b) -> a));
        BigDecimal maxScore = sumMarks(itemsFor(schoolId, quizId));
        Student learner = resolveLearner(principal, null);
        return students.stream()
                .filter(student -> learner == null || learner.getId().equals(student.getId()))
                .sorted(studentOrder(enrollments))
                .map(student -> toResultRow(student, enrollments.get(student.getId()),
                        bestAttempts.get(student.getId()), offline.get(student.getId()), quiz, maxScore, names))
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] exportResultsPdf(UUID quizId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        Quiz quiz = requireQuiz(quizId, schoolId);
        assertCanViewQuiz(SecurityUtils.currentPrincipal(), schoolId, quiz);
        String schoolName = schoolRepository.findById(schoolId).map(School::getName).orElse("");
        return pdfService.renderResults(schoolName, getQuiz(quizId), results(quizId));
    }

    @Transactional(readOnly = true)
    public byte[] exportAttemptPdf(UUID attemptId) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        QuizAttempt attempt = requireAttempt(attemptId, schoolId);
        Quiz quiz = requireQuiz(attempt.getQuizId(), schoolId);
        assertCanViewAttempt(SecurityUtils.currentPrincipal(), schoolId, quiz, attempt);
        String schoolName = schoolRepository.findById(schoolId).map(School::getName).orElse("");
        return pdfService.renderAttempt(schoolName, getQuiz(quiz.getId()), getAttempt(attemptId));
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

    private QuizAttemptDto persistAnswers(UUID attemptId, QuizSubmitRequest request, boolean submit) {
        UUID schoolId = SecurityUtils.currentSchoolId();
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        QuizAttempt attempt = requireAttempt(attemptId, schoolId);
        Quiz quiz = requireQuiz(attempt.getQuizId(), schoolId);
        Student student = requireAccessibleStudent(principal, schoolId, attempt.getStudentId(), true);
        if (!student.getId().equals(attempt.getStudentId()) && !canManage()) {
            throw AuthException.accessDenied();
        }
        if (attempt.getStatus() != QuizAttemptStatus.IN_PROGRESS) {
            throw new BusinessException("quiz.attempt_not_open");
        }
        Instant now = Instant.now();
        boolean timedOut = !now.isBefore(attempt.getExpiresAt());
        List<QuizItem> items = itemsFor(schoolId, quiz.getId());
        Map<UUID, QuizItem> itemByQuestion = items.stream()
                .collect(Collectors.toMap(QuizItem::getQuestionId, Function.identity(), (a, b) -> a));
        Map<UUID, QuizAnswer> existing = answersFor(schoolId, attempt.getId()).stream()
                .collect(Collectors.toMap(QuizAnswer::getQuestionId, Function.identity(), (a, b) -> a));
        if (request.answers() != null) {
            for (QuizSubmitRequest.Answer row : request.answers()) {
                if (!itemByQuestion.containsKey(row.questionId())) {
                    throw new BusinessException("quiz.question_not_in_quiz");
                }
                QuizAnswer answer = existing.computeIfAbsent(row.questionId(), questionId -> {
                    QuizAnswer created = new QuizAnswer();
                    created.setSchoolId(schoolId);
                    created.setAttemptId(attempt.getId());
                    created.setQuestionId(questionId);
                    return created;
                });
                answer.setResponse(blankToNull(row.response()));
                answerRepository.save(answer);
            }
        }
        if (submit || timedOut) {
            finalizeAttempt(attempt, items, timedOut && !submit);
        }
        return getAttempt(attempt.getId());
    }

    private void finalizeAttempt(QuizAttempt attempt, List<QuizItem> items, boolean timedOut) {
        if (items.isEmpty()) {
            items = itemsFor(attempt.getSchoolId(), attempt.getQuizId());
        }
        List<QuizAnswer> answers = answersFor(attempt.getSchoolId(), attempt.getId());
        Map<UUID, QuizQuestion> questions = questionsFor(attempt.getSchoolId(), items);
        autoGrade(items, answers, questions);
        scoreAttempt(attempt, items, answers);
        boolean pendingManual = items.stream().anyMatch(item -> {
            QuizQuestion question = questions.get(item.getQuestionId());
            return question != null && question.getQuestionType() == QuizQuestionType.SHORT_ANSWER;
        });
        attempt.setSubmittedAt(Instant.now());
        attempt.setStatus(timedOut
                ? QuizAttemptStatus.TIMED_OUT
                : (pendingManual ? QuizAttemptStatus.SUBMITTED : QuizAttemptStatus.GRADED));
        attemptRepository.save(attempt);
    }

    private void autoGrade(List<QuizItem> items, List<QuizAnswer> answers, Map<UUID, QuizQuestion> questions) {
        Map<UUID, QuizAnswer> byQuestion = answers.stream()
                .collect(Collectors.toMap(QuizAnswer::getQuestionId, Function.identity(), (a, b) -> a));
        for (QuizItem item : items) {
            QuizQuestion question = questions.get(item.getQuestionId());
            if (question == null) {
                continue;
            }
            QuizAnswer answer = byQuestion.get(item.getQuestionId());
            if (answer == null) {
                continue;
            }
            if (question.getQuestionType() == QuizQuestionType.SHORT_ANSWER) {
                continue;
            }
            boolean correct = matches(question.getCorrectAnswer(), answer.getResponse());
            answer.setAutoCorrect(correct);
            answer.setAutoMarks(correct ? item.getMarks() : BigDecimal.ZERO);
            answer.setAwardedMarks(answer.getAutoMarks());
            answerRepository.save(answer);
        }
    }

    private void scoreAttempt(QuizAttempt attempt, List<QuizItem> items, List<QuizAnswer> answers) {
        Map<UUID, QuizQuestion> questions = questionsFor(attempt.getSchoolId(), items);
        BigDecimal auto = BigDecimal.ZERO;
        BigDecimal manual = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (QuizAnswer answer : answers) {
            QuizQuestion question = questions.get(answer.getQuestionId());
            BigDecimal awarded = answer.getAwardedMarks() == null ? BigDecimal.ZERO : answer.getAwardedMarks();
            total = total.add(awarded);
            if (question != null && question.getQuestionType() == QuizQuestionType.SHORT_ANSWER) {
                manual = manual.add(awarded);
            } else {
                auto = auto.add(awarded);
            }
        }
        BigDecimal max = sumMarks(items);
        attempt.setAutoScore(auto);
        attempt.setManualScore(manual);
        attempt.setTotalScore(total);
        attempt.setMaxScore(max);
        attempt.setPercentage(percent(total, max));
    }

    private QuizTakeDto toTakeDto(Quiz quiz, QuizAttempt attempt, UUID schoolId) {
        List<QuizItem> items = itemsFor(schoolId, quiz.getId());
        Map<UUID, QuizQuestion> questions = questionsFor(schoolId, items);
        Map<UUID, QuizAnswer> answers = answersFor(schoolId, attempt.getId()).stream()
                .collect(Collectors.toMap(QuizAnswer::getQuestionId, Function.identity(), (a, b) -> a));
        List<QuizTakeQuestionDto> takeQuestions = new ArrayList<>();
        for (QuizItem item : items) {
            QuizQuestion question = questions.get(item.getQuestionId());
            if (question == null) {
                continue;
            }
            QuizAnswer answer = answers.get(item.getQuestionId());
            takeQuestions.add(new QuizTakeQuestionDto(
                    question.getId(),
                    question.getQuestionType().name(),
                    question.getPrompt(),
                    QuizOptionsJson.fromJson(question.getOptionsJson()),
                    item.getMarks(),
                    answer == null ? null : answer.getResponse()));
        }
        if (quiz.isShuffleQuestions()) {
            java.util.Collections.shuffle(takeQuestions);
        }
        return new QuizTakeDto(attempt.getId(), quiz.getId(), quiz.getTitle(), quiz.getInstructions(),
                quiz.getDurationMinutes(), attempt.getStartedAt(), attempt.getExpiresAt(),
                attempt.getStatus().name(), takeQuestions);
    }

    private List<QuizDto> toQuizDtos(UUID schoolId, List<Quiz> quizzes, boolean includeItems) {
        Names names = names(schoolId);
        Map<UUID, List<QuizItem>> items = itemRepository.findBySchoolIdAndQuizIdInOrderBySortOrderAscCreatedAtAsc(
                        schoolId, quizzes.stream().map(Quiz::getId).toList()).stream()
                .collect(Collectors.groupingBy(QuizItem::getQuizId));
        Map<UUID, Long> attemptCounts = attemptRepository.findBySchoolIdAndQuizIdIn(
                        schoolId, quizzes.stream().map(Quiz::getId).toList()).stream()
                .collect(Collectors.groupingBy(QuizAttempt::getQuizId, Collectors.counting()));
        boolean reveal = canManage();
        return quizzes.stream()
                .map(quiz -> toQuizDto(quiz, names, items.getOrDefault(quiz.getId(), List.of()),
                        attemptCounts.getOrDefault(quiz.getId(), 0L).intValue(), includeItems && reveal))
                .toList();
    }

    private QuizDto toQuizDto(Quiz quiz, Names names, List<QuizItem> items, int attemptCount, boolean includeItems) {
        List<QuizItemDto> itemDtos = List.of();
        if (includeItems) {
            Map<UUID, QuizQuestion> questions = questionsFor(quiz.getSchoolId(), items);
            itemDtos = items.stream()
                    .map(item -> new QuizItemDto(item.getId(), item.getQuestionId(), item.getSortOrder(),
                            item.getMarks(), toQuestionDto(questions.get(item.getQuestionId()), names, canManage())))
                    .toList();
        }
        return new QuizDto(
                quiz.getId(), quiz.getAcademicYearId(), names.yearName(quiz.getAcademicYearId()),
                quiz.getClassId(), names.className(quiz.getClassId()),
                quiz.getSectionId(), names.sectionName(quiz.getSectionId()),
                quiz.getSubjectId(), names.subjectName(quiz.getSubjectId()),
                quiz.getTitle(), quiz.getInstructions(), quiz.getDurationMinutes(), quiz.getMaxAttempts(),
                quiz.getStartAt(), quiz.getEndAt(), quiz.getPassMarks(), quiz.isShuffleQuestions(),
                quiz.isShowResults(), quiz.getStatus().name(), items.size(), sumMarks(items),
                attemptCount, quiz.getCreatedAt(), itemDtos);
    }

    private List<QuizQuestionDto> toQuestionDtos(UUID schoolId, List<QuizQuestion> questions, boolean revealAnswer) {
        Names names = names(schoolId);
        return questions.stream().map(q -> toQuestionDto(q, names, revealAnswer)).toList();
    }

    private QuizQuestionDto toQuestionDto(QuizQuestion question, Names names, boolean revealAnswer) {
        if (question == null) {
            return null;
        }
        return new QuizQuestionDto(
                question.getId(),
                question.getSubjectId(), names.subjectName(question.getSubjectId()),
                question.getClassId(), names.className(question.getClassId()),
                question.getQuestionType().name(),
                question.getPrompt(),
                QuizOptionsJson.fromJson(question.getOptionsJson()),
                revealAnswer ? question.getCorrectAnswer() : null,
                question.getMarks(),
                revealAnswer ? question.getExplanation() : null,
                question.getCreatedAt());
    }

    private QuizAttemptDto toAttemptDto(QuizAttempt attempt, Quiz quiz, Names names,
                                        List<QuizAnswer> answers, boolean reveal) {
        Student student = names.student(attempt.getStudentId());
        StudentEnrollment enrollment = names.enrollment(attempt.getStudentId());
        Map<UUID, QuizItem> items = itemsFor(attempt.getSchoolId(), attempt.getQuizId()).stream()
                .collect(Collectors.toMap(QuizItem::getQuestionId, Function.identity(), (a, b) -> a));
        Map<UUID, QuizQuestion> questions = questionsFor(attempt.getSchoolId(), new ArrayList<>(items.values()));
        List<QuizAnswerDto> answerDtos = answers.stream()
                .map(answer -> {
                    QuizQuestion question = questions.get(answer.getQuestionId());
                    QuizItem item = items.get(answer.getQuestionId());
                    return new QuizAnswerDto(
                            answer.getQuestionId(),
                            question == null ? null : question.getQuestionType().name(),
                            question == null ? null : question.getPrompt(),
                            question == null ? List.of() : QuizOptionsJson.fromJson(question.getOptionsJson()),
                            reveal && question != null ? question.getCorrectAnswer() : null,
                            answer.getResponse(),
                            reveal ? answer.getAutoCorrect() : null,
                            reveal ? answer.getAutoMarks() : null,
                            reveal ? answer.getAwardedMarks() : null,
                            item == null ? null : item.getMarks(),
                            reveal ? answer.getGraderComment() : null);
                })
                .toList();
        boolean passed = quiz.getPassMarks() == null || (attempt.getTotalScore() != null
                && attempt.getTotalScore().compareTo(quiz.getPassMarks()) >= 0);
        return new QuizAttemptDto(
                attempt.getId(), attempt.getQuizId(), quiz.getTitle(),
                attempt.getStudentId(),
                student == null ? null : student.getDisplayName(),
                student == null ? null : student.getAdmissionNo(),
                enrollment == null ? null : enrollment.getRollNumber(),
                attempt.getAttemptNo(), attempt.getStatus().name(),
                attempt.getStartedAt(), attempt.getSubmittedAt(), attempt.getExpiresAt(),
                reveal ? attempt.getAutoScore() : null,
                reveal ? attempt.getManualScore() : null,
                reveal ? attempt.getTotalScore() : null,
                attempt.getMaxScore(),
                reveal ? attempt.getPercentage() : null,
                reveal && attempt.getTotalScore() != null && passed,
                answerDtos);
    }

    private QuizResultRowDto toResultRow(Student student, StudentEnrollment enrollment, QuizAttempt attempt,
                                         QuizOfflineMark offline, Quiz quiz, BigDecimal maxScore, Names names) {
        BigDecimal score = null;
        String source = "NONE";
        String status = null;
        Instant submittedAt = null;
        UUID attemptId = null;
        int attemptNo = 0;
        if (attempt != null && attempt.getTotalScore() != null) {
            score = attempt.getTotalScore();
            source = "ONLINE";
            status = attempt.getStatus().name();
            submittedAt = attempt.getSubmittedAt();
            attemptId = attempt.getId();
            attemptNo = attempt.getAttemptNo();
        } else if (offline != null && offline.getMarks() != null) {
            score = offline.getMarks();
            source = "OFFLINE";
            status = "OFFLINE";
        }
        BigDecimal percent = percent(score, maxScore);
        boolean passed = score != null && (quiz.getPassMarks() == null || score.compareTo(quiz.getPassMarks()) >= 0);
        return new QuizResultRowDto(
                student.getId(), student.getAdmissionNo(), student.getDisplayName(),
                enrollment == null ? null : enrollment.getRollNumber(),
                attemptId, attemptNo, source, status, submittedAt, score, maxScore, percent, passed);
    }

    private void applyQuestion(QuizQuestion question, QuizQuestionRequest request, UUID schoolId, TeacherScope scope) {
        QuizQuestionType type = parseQuestionType(request.questionType(), true);
        if (request.subjectId() != null) {
            Subject subject = subjectRepository.findByIdAndSchoolId(request.subjectId(), schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("subject", request.subjectId()));
            assertSubjectAllowed(scope, subject.getId());
            question.setSubjectId(subject.getId());
        } else {
            question.setSubjectId(null);
        }
        if (request.classId() != null) {
            SchoolClass klass = classRepository.findByIdAndSchoolId(request.classId(), schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("class", request.classId()));
            question.setClassId(klass.getId());
        } else {
            question.setClassId(null);
        }
        if (request.marks() == null || request.marks().signum() <= 0) {
            throw new BusinessException("quiz.invalid_marks");
        }
        if (type != QuizQuestionType.SHORT_ANSWER) {
            List<QuestionOptionDto> options = request.options() == null ? List.of() : request.options();
            if (options.size() < 2) {
                throw new BusinessException("quiz.options_required");
            }
            if (blankToNull(request.correctAnswer()) == null) {
                throw new BusinessException("quiz.correct_answer_required");
            }
            question.setOptionsJson(QuizOptionsJson.toJson(options));
        } else {
            question.setOptionsJson(null);
        }
        question.setQuestionType(type);
        question.setPrompt(request.prompt().trim());
        question.setCorrectAnswer(blankToNull(request.correctAnswer()));
        question.setMarks(request.marks());
        question.setExplanation(blankToNull(request.explanation()));
    }

    private void applyQuiz(Quiz quiz, QuizRequest request, UUID schoolId, TeacherScope scope) {
        AcademicYear year = academicYearRepository.findByIdAndSchoolId(request.academicYearId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("academic_year", request.academicYearId()));
        SchoolClass klass = classRepository.findByIdAndSchoolId(request.classId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("class", request.classId()));
        Subject subject = subjectRepository.findByIdAndSchoolId(request.subjectId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("subject", request.subjectId()));
        assertSubjectAllowed(scope, subject.getId());
        UUID sectionId = request.sectionId();
        if (sectionId != null) {
            Section section = sectionRepository.findByIdAndSchoolId(sectionId, schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("section", sectionId));
            if (!klass.getId().equals(section.getClassId())) {
                throw new BusinessException("quiz.section_class_mismatch");
            }
            if (!scope.allowsSection(section.getId())) {
                throw AuthException.accessDenied();
            }
            quiz.setSectionId(section.getId());
        } else {
            quiz.setSectionId(null);
        }
        if (request.durationMinutes() == null || request.durationMinutes() <= 0) {
            throw new BusinessException("quiz.invalid_duration");
        }
        int attempts = request.maxAttempts() == null ? 1 : request.maxAttempts();
        if (attempts <= 0) {
            throw new BusinessException("quiz.invalid_attempts");
        }
        if (request.startAt() != null && request.endAt() != null && request.endAt().isBefore(request.startAt())) {
            throw new BusinessException("quiz.invalid_window");
        }
        quiz.setAcademicYearId(year.getId());
        quiz.setClassId(klass.getId());
        quiz.setSubjectId(subject.getId());
        quiz.setTitle(request.title().trim());
        quiz.setInstructions(blankToNull(request.instructions()));
        quiz.setDurationMinutes(request.durationMinutes());
        quiz.setMaxAttempts(attempts);
        quiz.setStartAt(request.startAt());
        quiz.setEndAt(request.endAt());
        quiz.setPassMarks(request.passMarks());
        quiz.setShuffleQuestions(request.shuffleQuestions());
        quiz.setShowResults(request.showResults() == null || request.showResults());
    }

    private void replaceItems(Quiz quiz, List<QuizItemRequest> requests, UUID schoolId, TeacherScope scope) {
        itemRepository.deleteByQuizIdAndSchoolId(quiz.getId(), schoolId);
        if (requests == null || requests.isEmpty()) {
            return;
        }
        int order = 1;
        for (QuizItemRequest request : requests) {
            QuizQuestion question = requireQuestion(request.questionId(), schoolId);
            if (question.getSubjectId() != null) {
                assertSubjectAllowed(scope, question.getSubjectId());
            }
            QuizItem item = new QuizItem();
            item.setSchoolId(schoolId);
            item.setQuizId(quiz.getId());
            item.setQuestionId(question.getId());
            item.setSortOrder(request.sortOrder() == null ? order : request.sortOrder());
            item.setMarks(request.marks() == null ? question.getMarks() : request.marks());
            if (item.getMarks() == null || item.getMarks().signum() <= 0) {
                throw new BusinessException("quiz.invalid_marks");
            }
            itemRepository.save(item);
            order++;
        }
    }

    private List<Student> roster(UUID schoolId, Quiz quiz) {
        if (quiz.getSectionId() != null) {
            return studentRepository.findBySectionAndYear(schoolId, quiz.getSectionId(), quiz.getAcademicYearId());
        }
        List<UUID> sectionIds = sectionRepository.findBySchoolIdAndClassIdOrderByNameAsc(schoolId, quiz.getClassId())
                .stream().map(Section::getId).toList();
        if (sectionIds.isEmpty()) {
            return List.of();
        }
        List<StudentEnrollment> enrollments = enrollmentRepository
                .findBySchoolIdAndAcademicYearIdAndStatusAndSectionIdIn(
                        schoolId, quiz.getAcademicYearId(), "ACTIVE", sectionIds);
        List<UUID> studentIds = enrollments.stream().map(StudentEnrollment::getStudentId).distinct().toList();
        if (studentIds.isEmpty()) {
            return List.of();
        }
        return studentRepository.findBySchoolIdAndIdIn(schoolId, studentIds);
    }

    private Map<UUID, StudentEnrollment> enrollmentsFor(UUID schoolId, Quiz quiz, List<Student> students) {
        Map<UUID, StudentEnrollment> result = new HashMap<>();
        for (Student student : students) {
            enrollmentRepository.findByStudentIdAndAcademicYearId(student.getId(), quiz.getAcademicYearId())
                    .ifPresent(enrollment -> result.put(student.getId(), enrollment));
        }
        return result;
    }

    private Map<UUID, QuizAttempt> bestAttempts(UUID schoolId, UUID quizId) {
        Map<UUID, QuizAttempt> best = new HashMap<>();
        for (QuizAttempt attempt : attemptRepository.findBySchoolIdAndQuizIdOrderByAttemptNoAsc(schoolId, quizId)) {
            if (attempt.getStatus() == QuizAttemptStatus.IN_PROGRESS) {
                continue;
            }
            QuizAttempt current = best.get(attempt.getStudentId());
            if (current == null || compareScore(attempt.getTotalScore(), current.getTotalScore()) > 0) {
                best.put(attempt.getStudentId(), attempt);
            }
        }
        return best;
    }

    private StudentEnrollment currentEnrollment(UUID schoolId, UUID studentId) {
        return academicYearRepository.findBySchoolIdAndCurrentTrue(schoolId)
                .flatMap(year -> enrollmentRepository.findByStudentIdAndAcademicYearId(studentId, year.getId()))
                .orElse(null);
    }

    private boolean visibleToLearner(Quiz quiz, StudentEnrollment enrollment) {
        if (quiz.getStatus() != QuizStatus.PUBLISHED && quiz.getStatus() != QuizStatus.CLOSED) {
            return false;
        }
        Section section = sectionRepository.findById(enrollment.getSectionId()).orElse(null);
        if (section == null) {
            return false;
        }
        if (!quiz.getClassId().equals(section.getClassId())) {
            return false;
        }
        if (quiz.getSectionId() != null && !quiz.getSectionId().equals(section.getId())) {
            return false;
        }
        return quiz.getAcademicYearId().equals(enrollment.getAcademicYearId());
    }

    private void assertLearnerCanTake(Quiz quiz, Student student) {
        StudentEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndAcademicYearId(student.getId(), quiz.getAcademicYearId())
                .orElseThrow(() -> new BusinessException("quiz.not_enrolled"));
        if (!visibleToLearner(quiz, enrollment) && quiz.getStatus() == QuizStatus.PUBLISHED) {
            Section section = sectionRepository.findById(enrollment.getSectionId()).orElse(null);
            if (section == null || !quiz.getClassId().equals(section.getClassId())
                    || (quiz.getSectionId() != null && !quiz.getSectionId().equals(section.getId()))) {
                throw new BusinessException("quiz.not_assigned");
            }
        }
        if (!visibleToLearner(quiz, enrollment) && quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new BusinessException("quiz.not_assigned");
        }
    }

    private Student requireAccessibleStudent(UserPrincipal principal, UUID schoolId, UUID studentId, boolean taking) {
        if (isLearner(principal)) {
            Student own = requireOwnStudent(principal, studentId);
            if (studentId != null && !own.getId().equals(studentId)) {
                throw AuthException.accessDenied();
            }
            if (taking && principal.hasRole("PARENT") && !principal.hasRole("STUDENT")) {
                throw AuthException.accessDenied();
            }
            return own;
        }
        UUID id = studentId == null ? requireOwnStudent(principal, null).getId() : studentId;
        return studentRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("student", id));
    }

    private Student requireOwnStudent(UserPrincipal principal, UUID preferredStudentId) {
        if (principal.hasRole("PARENT") && !principal.hasRole("STUDENT")
                && !principal.hasRole("ADMIN") && !principal.hasRole("SUPER_ADMIN")
                && !principal.hasRole("TEACHER")) {
            Guardian guardian = guardianRepository.findBySchoolIdAndUserId(principal.schoolId(), principal.id())
                    .orElseThrow(AuthException::accessDenied);
            List<StudentGuardian> links = studentGuardianRepository.findWithStudents(principal.schoolId(), guardian.getId());
            if (links.isEmpty()) {
                throw AuthException.accessDenied();
            }
            if (preferredStudentId != null) {
                return links.stream()
                        .map(StudentGuardian::getStudent)
                        .filter(student -> student != null && preferredStudentId.equals(student.getId()))
                        .findFirst()
                        .orElseThrow(AuthException::accessDenied);
            }
            return links.get(0).getStudent();
        }
        if (!principal.hasRole("STUDENT")) {
            throw AuthException.accessDenied();
        }
        return studentRepository.findBySchoolIdAndUserId(principal.schoolId(), principal.id())
                .orElseThrow(AuthException::accessDenied);
    }

    private Student resolveLearner(UserPrincipal principal, UUID studentId) {
        if (!isLearner(principal)) {
            return null;
        }
        return requireOwnStudent(principal, studentId);
    }

    private void assertCanViewQuiz(UserPrincipal principal, UUID schoolId, Quiz quiz) {
        if (isLearner(principal)) {
            Student student = requireOwnStudent(principal, null);
            StudentEnrollment enrollment = enrollmentRepository
                    .findByStudentIdAndAcademicYearId(student.getId(), quiz.getAcademicYearId())
                    .orElse(null);
            if (enrollment == null || !visibleToLearner(quiz, enrollment)) {
                throw AuthException.accessDenied();
            }
            return;
        }
        TeacherScope scope = teacherScope(schoolId, principal);
        if (!scope.allowsSection(quiz.getSectionId()) || !scope.allowsSubject(quiz.getSubjectId())) {
            throw AuthException.accessDenied();
        }
    }

    private void assertCanViewAttempt(UserPrincipal principal, UUID schoolId, Quiz quiz, QuizAttempt attempt) {
        if (isLearner(principal)) {
            Student student = requireOwnStudent(principal, attempt.getStudentId());
            if (!student.getId().equals(attempt.getStudentId())) {
                throw AuthException.accessDenied();
            }
            return;
        }
        assertCanViewQuiz(principal, schoolId, quiz);
    }

    private TeacherScope teacherScope(UUID schoolId, UserPrincipal principal) {
        if (principal.hasRole("ADMIN") || principal.hasRole("SUPER_ADMIN") || isLearner(principal)) {
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

    private void assertSubjectAllowed(TeacherScope scope, UUID subjectId) {
        if (!scope.allowsSubject(subjectId)) {
            throw AuthException.accessDenied();
        }
    }

    private QuizQuestion requireQuestion(UUID id, UUID schoolId) {
        return questionRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("quiz_question", id));
    }

    private Quiz requireQuiz(UUID id, UUID schoolId) {
        return quizRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("quiz", id));
    }

    private QuizAttempt requireAttempt(UUID id, UUID schoolId) {
        return attemptRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("quiz_attempt", id));
    }

    private List<QuizItem> itemsFor(UUID schoolId, UUID quizId) {
        return itemRepository.findBySchoolIdAndQuizIdOrderBySortOrderAscCreatedAtAsc(schoolId, quizId);
    }

    private List<QuizAnswer> answersFor(UUID schoolId, UUID attemptId) {
        return answerRepository.findBySchoolIdAndAttemptId(schoolId, attemptId);
    }

    private Map<UUID, List<QuizAnswer>> answersByAttempt(UUID schoolId, List<QuizAttempt> attempts) {
        List<UUID> ids = attempts.stream().map(QuizAttempt::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return answerRepository.findBySchoolIdAndAttemptIdIn(schoolId, ids).stream()
                .collect(Collectors.groupingBy(QuizAnswer::getAttemptId));
    }

    private Map<UUID, QuizQuestion> questionsFor(UUID schoolId, List<QuizItem> items) {
        List<UUID> ids = items.stream().map(QuizItem::getQuestionId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return questionRepository.findBySchoolIdAndIdIn(schoolId, ids).stream()
                .collect(Collectors.toMap(QuizQuestion::getId, Function.identity()));
    }

    private Names names(UUID schoolId) {
        Map<UUID, AcademicYear> years = academicYearRepository.findBySchoolIdOrderByStartDateDesc(schoolId)
                .stream().collect(Collectors.toMap(AcademicYear::getId, Function.identity()));
        Map<UUID, SchoolClass> classes = classRepository.findBySchoolIdOrderBySortOrderAsc(schoolId)
                .stream().collect(Collectors.toMap(SchoolClass::getId, Function.identity()));
        Map<UUID, Section> sections = sectionRepository.findBySchoolIdOrderByNameAsc(schoolId)
                .stream().collect(Collectors.toMap(Section::getId, Function.identity()));
        Map<UUID, Subject> subjects = subjectRepository.findBySchoolIdOrderByNameAsc(schoolId)
                .stream().collect(Collectors.toMap(Subject::getId, Function.identity()));
        return new Names(years, classes, sections, subjects, studentRepository, enrollmentRepository, schoolId);
    }

    private QuizQuestionType parseQuestionType(String value, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) {
                throw new BusinessException("quiz.invalid_type");
            }
            return null;
        }
        try {
            return QuizQuestionType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("quiz.invalid_type");
        }
    }

    private QuizStatus parseStatus(String value, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) {
                throw new BusinessException("quiz.invalid_status");
            }
            return null;
        }
        try {
            return QuizStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("quiz.invalid_status");
        }
    }

    private boolean isLearner(UserPrincipal principal) {
        return (principal.hasRole("STUDENT") || principal.hasRole("PARENT"))
                && !principal.hasRole("ADMIN") && !principal.hasRole("SUPER_ADMIN") && !principal.hasRole("TEACHER");
    }

    private void assertCanManage() {
        if (!canManage()) {
            throw AuthException.accessDenied();
        }
    }

    private boolean canManage() {
        return SecurityUtils.currentPrincipal().permissions().contains("QUIZ_MANAGE");
    }

    private static BigDecimal sumMarks(List<QuizItem> items) {
        return items.stream()
                .map(item -> item.getMarks() == null ? BigDecimal.ZERO : item.getMarks())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal percent(BigDecimal score, BigDecimal max) {
        if (score == null || max == null || max.signum() == 0) {
            return null;
        }
        return score.multiply(BigDecimal.valueOf(100)).divide(max, 2, RoundingMode.HALF_UP);
    }

    private static boolean matches(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return expected.trim().equalsIgnoreCase(actual.trim());
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int compareScore(BigDecimal left, BigDecimal right) {
        BigDecimal a = left == null ? BigDecimal.ZERO : left;
        BigDecimal b = right == null ? BigDecimal.ZERO : right;
        return a.compareTo(b);
    }

    private static Comparator<Student> studentOrder(Map<UUID, StudentEnrollment> enrollments) {
        return Comparator
                .comparing((Student student) -> {
                    StudentEnrollment enrollment = enrollments.get(student.getId());
                    return enrollment == null || enrollment.getRollNumber() == null
                            ? Integer.MAX_VALUE : enrollment.getRollNumber();
                })
                .thenComparing(Student::getLastName, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(Student::getFirstName, Comparator.nullsLast(String::compareToIgnoreCase));
    }

    private static boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static List<QuizQuestionDto> sortQuestions(List<QuizQuestionDto> questions, String sort, String direction) {
        boolean desc = "desc".equalsIgnoreCase(direction);
        Comparator<QuizQuestionDto> cmp = switch (sort == null ? "" : sort) {
            case "prompt" -> Comparator.comparing(q -> nullToEmpty(q.prompt()), String.CASE_INSENSITIVE_ORDER);
            case "questionType" -> Comparator.comparing(q -> nullToEmpty(q.questionType()), String.CASE_INSENSITIVE_ORDER);
            case "subject" -> Comparator.comparing(q -> nullToEmpty(q.subjectName()), String.CASE_INSENSITIVE_ORDER);
            case "class" -> Comparator.comparing(q -> nullToEmpty(q.className()), String.CASE_INSENSITIVE_ORDER);
            case "marks" -> Comparator.comparing(q -> q.marks() == null ? BigDecimal.ZERO : q.marks());
            default -> Comparator.comparing(QuizQuestionDto::createdAt, Comparator.nullsLast(Comparator.naturalOrder()));
        };
        if (desc || ((sort == null || sort.isBlank()) && !"asc".equalsIgnoreCase(direction))) {
            cmp = cmp.reversed();
        }
        return questions.stream().sorted(cmp).toList();
    }

    private static List<QuizDto> sortQuizzes(List<QuizDto> quizzes, String sort, String direction) {
        boolean desc = "desc".equalsIgnoreCase(direction);
        Comparator<QuizDto> cmp = switch (sort == null ? "" : sort) {
            case "title" -> Comparator.comparing(q -> nullToEmpty(q.title()), String.CASE_INSENSITIVE_ORDER);
            case "class" -> Comparator.comparing(q -> nullToEmpty(q.className()), String.CASE_INSENSITIVE_ORDER);
            case "subject" -> Comparator.comparing(q -> nullToEmpty(q.subjectName()), String.CASE_INSENSITIVE_ORDER);
            case "status" -> Comparator.comparing(q -> nullToEmpty(q.status()), String.CASE_INSENSITIVE_ORDER);
            case "startAt" -> Comparator.comparing(QuizDto::startAt, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(QuizDto::createdAt, Comparator.nullsLast(Comparator.naturalOrder()));
        };
        if (desc || ((sort == null || sort.isBlank()) && !"asc".equalsIgnoreCase(direction))) {
            cmp = cmp.reversed();
        }
        return quizzes.stream().sorted(cmp).toList();
    }

    private record TeacherScope(Set<UUID> sections, Set<UUID> subjects) {
        static TeacherScope all() {
            return new TeacherScope(null, null);
        }

        boolean allowsSection(UUID sectionId) {
            return sections == null || sections.isEmpty() || sectionId == null || sections.contains(sectionId);
        }

        boolean allowsSubject(UUID subjectId) {
            return subjects == null || subjects.isEmpty() || subjectId == null || subjects.contains(subjectId);
        }
    }

    private static final class Names {
        private final Map<UUID, AcademicYear> years;
        private final Map<UUID, SchoolClass> classes;
        private final Map<UUID, Section> sections;
        private final Map<UUID, Subject> subjects;
        private final StudentRepository studentRepository;
        private final StudentEnrollmentRepository enrollmentRepository;
        private final UUID schoolId;
        private final Map<UUID, Student> students = new HashMap<>();
        private final Map<UUID, StudentEnrollment> enrollments = new HashMap<>();

        Names(Map<UUID, AcademicYear> years, Map<UUID, SchoolClass> classes, Map<UUID, Section> sections,
              Map<UUID, Subject> subjects, StudentRepository studentRepository,
              StudentEnrollmentRepository enrollmentRepository, UUID schoolId) {
            this.years = years;
            this.classes = classes;
            this.sections = sections;
            this.subjects = subjects;
            this.studentRepository = studentRepository;
            this.enrollmentRepository = enrollmentRepository;
            this.schoolId = schoolId;
        }

        String yearName(UUID id) {
            AcademicYear year = years.get(id);
            return year == null ? null : year.getName();
        }

        String className(UUID id) {
            SchoolClass klass = classes.get(id);
            return klass == null ? null : klass.getName();
        }

        String sectionName(UUID id) {
            Section section = sections.get(id);
            return section == null ? null : section.getName();
        }

        String subjectName(UUID id) {
            Subject subject = subjects.get(id);
            return subject == null ? null : subject.getName();
        }

        Student student(UUID id) {
            return students.computeIfAbsent(id, key -> studentRepository.findByIdAndSchoolId(key, schoolId).orElse(null));
        }

        StudentEnrollment enrollment(UUID studentId) {
            return enrollments.computeIfAbsent(studentId, key ->
                    enrollmentRepository.findByStudentId(key).stream().findFirst().orElse(null));
        }
    }
}
