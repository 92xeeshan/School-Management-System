package com.schoolms.quiz;

import com.schoolms.TestSecurity;
import com.schoolms.academics.AcademicYear;
import com.schoolms.academics.AcademicYearRepository;
import com.schoolms.academics.ClassSubjectRepository;
import com.schoolms.academics.SchoolClass;
import com.schoolms.academics.SchoolClassRepository;
import com.schoolms.academics.Section;
import com.schoolms.academics.SectionRepository;
import com.schoolms.academics.StudentEnrollment;
import com.schoolms.academics.StudentEnrollmentRepository;
import com.schoolms.academics.Subject;
import com.schoolms.academics.SubjectRepository;
import com.schoolms.academics.TeacherProfileRepository;
import com.schoolms.academics.TeacherSectionRepository;
import com.schoolms.academics.TeacherSubjectRepository;
import com.schoolms.common.enums.QuizAttemptStatus;
import com.schoolms.common.enums.QuizQuestionType;
import com.schoolms.common.enums.QuizStatus;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.quiz.dto.QuestionOptionDto;
import com.schoolms.quiz.dto.QuizAttemptDto;
import com.schoolms.quiz.dto.QuizGradeRequest;
import com.schoolms.quiz.dto.QuizItemRequest;
import com.schoolms.quiz.dto.QuizOfflineGridDto;
import com.schoolms.quiz.dto.QuizOfflineSaveRequest;
import com.schoolms.quiz.dto.QuizQuestionDto;
import com.schoolms.quiz.dto.QuizQuestionRequest;
import com.schoolms.quiz.dto.QuizRequest;
import com.schoolms.quiz.dto.QuizStatusRequest;
import com.schoolms.quiz.dto.QuizSubmitRequest;
import com.schoolms.quiz.dto.QuizTakeDto;
import com.schoolms.school.SchoolRepository;
import com.schoolms.student.GuardianRepository;
import com.schoolms.student.Student;
import com.schoolms.student.StudentGuardianRepository;
import com.schoolms.student.StudentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock private QuizQuestionRepository questionRepository;
    @Mock private QuizRepository quizRepository;
    @Mock private QuizItemRepository itemRepository;
    @Mock private QuizAttemptRepository attemptRepository;
    @Mock private QuizAnswerRepository answerRepository;
    @Mock private QuizOfflineMarkRepository offlineMarkRepository;
    @Mock private QuizPdfService pdfService;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private SchoolClassRepository classRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private ClassSubjectRepository classSubjectRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private StudentEnrollmentRepository enrollmentRepository;
    @Mock private TeacherProfileRepository teacherRepository;
    @Mock private TeacherSectionRepository teacherSectionRepository;
    @Mock private TeacherSubjectRepository teacherSubjectRepository;
    @Mock private GuardianRepository guardianRepository;
    @Mock private StudentGuardianRepository studentGuardianRepository;
    @Mock private SchoolRepository schoolRepository;

    private QuizService service;
    private UUID yearId;
    private UUID classId;
    private UUID sectionId;
    private UUID subjectId;
    private UUID studentId;
    private UUID quizId;
    private final Map<UUID, QuizQuestion> questions = new HashMap<>();
    private final Map<UUID, Quiz> quizzes = new HashMap<>();
    private final Map<UUID, QuizItem> items = new HashMap<>();
    private final Map<UUID, QuizAttempt> attempts = new HashMap<>();
    private final Map<UUID, QuizAnswer> answers = new HashMap<>();
    private final Map<UUID, QuizOfflineMark> offlineMarks = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new QuizService(questionRepository, quizRepository, itemRepository, attemptRepository,
                answerRepository, offlineMarkRepository, pdfService, academicYearRepository, classRepository,
                sectionRepository, subjectRepository, classSubjectRepository, studentRepository, enrollmentRepository,
                teacherRepository, teacherSectionRepository, teacherSubjectRepository, guardianRepository,
                studentGuardianRepository, schoolRepository);
        TestSecurity.login(TestSecurity.USER_ID, TestSecurity.SCHOOL_ID, List.of("ADMIN"),
                Set.of("QUIZ_READ", "QUIZ_MANAGE"));
        yearId = UUID.randomUUID();
        classId = UUID.randomUUID();
        sectionId = UUID.randomUUID();
        subjectId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        quizId = UUID.randomUUID();

        org.mockito.Mockito.lenient().when(questionRepository.save(any(QuizQuestion.class))).thenAnswer(persist(questions));
        org.mockito.Mockito.lenient().when(quizRepository.save(any(Quiz.class))).thenAnswer(persist(quizzes));
        org.mockito.Mockito.lenient().when(itemRepository.save(any(QuizItem.class))).thenAnswer(persist(items));
        org.mockito.Mockito.lenient().when(attemptRepository.save(any(QuizAttempt.class))).thenAnswer(persist(attempts));
        org.mockito.Mockito.lenient().when(answerRepository.save(any(QuizAnswer.class))).thenAnswer(persist(answers));
        org.mockito.Mockito.lenient().when(offlineMarkRepository.save(any(QuizOfflineMark.class))).thenAnswer(persist(offlineMarks));
    }

    @AfterEach
    void tearDown() {
        TestSecurity.clear();
    }

    @Test
    void createQuestionStoresMcqOptions() {
        stubLookups();
        QuizQuestionDto dto = service.createQuestion(new QuizQuestionRequest(
                subjectId, classId, "MCQ", "What is 2+2?",
                List.of(new QuestionOptionDto("A", "3"), new QuestionOptionDto("B", "4")),
                "B", BigDecimal.ONE, "2+2=4"));
        assertEquals("MCQ", dto.questionType());
        assertEquals("B", dto.correctAnswer());
        assertEquals(2, dto.options().size());
    }

    @Test
    void publishRequiresQuestions() {
        stubLookups();
        Quiz quiz = quiz(QuizStatus.DRAFT);
        when(quizRepository.findByIdAndSchoolId(quiz.getId(), TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(quiz));
        when(itemRepository.countByQuizId(quiz.getId())).thenReturn(0L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateStatus(quiz.getId(), new QuizStatusRequest("PUBLISHED")));
        assertEquals("quiz.no_questions", ex.getCode());
    }

    @Test
    void submitAutoGradesMcqAndTrueFalse() {
        stubLookups();
        Quiz quiz = quiz(QuizStatus.PUBLISHED);
        QuizQuestion mcq = question(QuizQuestionType.MCQ, "B");
        QuizQuestion tf = question(QuizQuestionType.TRUE_FALSE, "TRUE");
        QuizQuestion shortQ = question(QuizQuestionType.SHORT_ANSWER, "19");
        QuizItem item1 = item(quiz.getId(), mcq, 2);
        QuizItem item2 = item(quiz.getId(), tf, 1);
        QuizItem item3 = item(quiz.getId(), shortQ, 2);
        QuizAttempt attempt = attempt(quiz, QuizAttemptStatus.IN_PROGRESS, Instant.now().plusSeconds(600));

        when(attemptRepository.findByIdAndSchoolId(attempt.getId(), TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(attempt));
        when(quizRepository.findByIdAndSchoolId(quiz.getId(), TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(quiz));
        when(studentRepository.findByIdAndSchoolId(studentId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(student()));
        when(itemRepository.findBySchoolIdAndQuizIdOrderBySortOrderAscCreatedAtAsc(
                TestSecurity.SCHOOL_ID, quiz.getId())).thenReturn(List.of(item1, item2, item3));
        when(questionRepository.findBySchoolIdAndIdIn(eq(TestSecurity.SCHOOL_ID), any()))
                .thenReturn(List.of(mcq, tf, shortQ));
        when(answerRepository.findBySchoolIdAndAttemptId(TestSecurity.SCHOOL_ID, attempt.getId()))
                .thenAnswer(inv -> new ArrayList<>(answers.values()));

        QuizAttemptDto result = service.submitAttempt(attempt.getId(), new QuizSubmitRequest(List.of(
                new QuizSubmitRequest.Answer(mcq.getId(), "B"),
                new QuizSubmitRequest.Answer(tf.getId(), "TRUE"),
                new QuizSubmitRequest.Answer(shortQ.getId(), "19"))));

        assertEquals(QuizAttemptStatus.SUBMITTED.name(), result.status());
        assertEquals(new BigDecimal("3"), result.autoScore());
        assertNotNull(result.submittedAt());
        QuizAnswer shortAnswer = answers.values().stream()
                .filter(a -> shortQ.getId().equals(a.getQuestionId())).findFirst().orElseThrow();
        assertNull(shortAnswer.getAutoMarks());
    }

    @Test
    void gradeShortAnswerCompletesAttempt() {
        stubLookups();
        Quiz quiz = quiz(QuizStatus.PUBLISHED);
        QuizQuestion shortQ = question(QuizQuestionType.SHORT_ANSWER, "19");
        QuizItem item = item(quiz.getId(), shortQ, 2);
        QuizAttempt attempt = attempt(quiz, QuizAttemptStatus.SUBMITTED, Instant.now().minusSeconds(10));
        QuizAnswer answer = new QuizAnswer();
        answer.setId(UUID.randomUUID());
        answer.setSchoolId(TestSecurity.SCHOOL_ID);
        answer.setAttemptId(attempt.getId());
        answer.setQuestionId(shortQ.getId());
        answer.setResponse("19");
        answers.put(answer.getId(), answer);

        when(attemptRepository.findByIdAndSchoolId(attempt.getId(), TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(attempt));
        when(quizRepository.findByIdAndSchoolId(quiz.getId(), TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(quiz));
        when(itemRepository.findBySchoolIdAndQuizIdOrderBySortOrderAscCreatedAtAsc(
                TestSecurity.SCHOOL_ID, quiz.getId())).thenReturn(List.of(item));
        when(questionRepository.findBySchoolIdAndIdIn(eq(TestSecurity.SCHOOL_ID), any())).thenReturn(List.of(shortQ));
        when(answerRepository.findBySchoolIdAndAttemptId(TestSecurity.SCHOOL_ID, attempt.getId()))
                .thenReturn(List.of(answer));
        when(studentRepository.findByIdAndSchoolId(studentId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(student()));
        when(enrollmentRepository.findByStudentId(studentId)).thenReturn(List.of());

        QuizAttemptDto result = service.gradeAttempt(attempt.getId(), new QuizGradeRequest(List.of(
                new QuizGradeRequest.GradeRow(shortQ.getId(), new BigDecimal("2"), "Correct"))));
        assertEquals(QuizAttemptStatus.GRADED.name(), result.status());
        assertEquals(new BigDecimal("2"), result.totalScore());
    }

    @Test
    void startTimesOutExpiredAttempt() {
        stubLookups();
        Quiz quiz = quiz(QuizStatus.PUBLISHED);
        quiz.setMaxAttempts(2);
        QuizQuestion mcq = question(QuizQuestionType.MCQ, "B");
        QuizItem item = item(quiz.getId(), mcq, 2);
        QuizAttempt expired = attempt(quiz, QuizAttemptStatus.IN_PROGRESS, Instant.now().minusSeconds(5));

        when(quizRepository.findByIdAndSchoolId(quiz.getId(), TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(quiz));
        when(studentRepository.findByIdAndSchoolId(studentId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(student()));
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(studentId, yearId))
                .thenReturn(Optional.of(enrollment()));
        when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section()));
        when(attemptRepository.findFirstBySchoolIdAndQuizIdAndStudentIdAndStatus(
                TestSecurity.SCHOOL_ID, quiz.getId(), studentId, QuizAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(expired));
        when(itemRepository.findBySchoolIdAndQuizIdOrderBySortOrderAscCreatedAtAsc(
                TestSecurity.SCHOOL_ID, quiz.getId())).thenReturn(List.of(item));
        when(questionRepository.findBySchoolIdAndIdIn(eq(TestSecurity.SCHOOL_ID), any())).thenReturn(List.of(mcq));
        when(answerRepository.findBySchoolIdAndAttemptId(TestSecurity.SCHOOL_ID, expired.getId())).thenReturn(List.of());
        when(attemptRepository.countByQuizIdAndStudentId(quiz.getId(), studentId)).thenReturn(1L);

        QuizTakeDto take = service.startAttempt(quiz.getId(), studentId);
        assertEquals(QuizAttemptStatus.TIMED_OUT, expired.getStatus());
        assertNotNull(take.attemptId());
        assertFalse(take.attemptId().equals(expired.getId()));
    }

    @Test
    void saveOfflineMarksForRoster() {
        stubLookups();
        Quiz quiz = quiz(QuizStatus.PUBLISHED);
        Student student = student();
        when(quizRepository.findByIdAndSchoolId(quiz.getId(), TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(quiz));
        when(studentRepository.findBySectionAndYear(TestSecurity.SCHOOL_ID, sectionId, yearId))
                .thenReturn(List.of(student));
        QuizQuestion mcq = question(QuizQuestionType.MCQ, "B");
        QuizItem item = item(quiz.getId(), mcq, 5);
        when(itemRepository.findBySchoolIdAndQuizIdOrderBySortOrderAscCreatedAtAsc(
                TestSecurity.SCHOOL_ID, quiz.getId())).thenReturn(List.of(item));
        when(offlineMarkRepository.findByQuizIdAndStudentId(quiz.getId(), studentId)).thenReturn(Optional.empty());
        when(offlineMarkRepository.findBySchoolIdAndQuizId(TestSecurity.SCHOOL_ID, quiz.getId()))
                .thenAnswer(inv -> new ArrayList<>(offlineMarks.values()));
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(studentId, yearId))
                .thenReturn(Optional.of(enrollment()));

        QuizOfflineGridDto grid = service.saveOffline(quiz.getId(), new QuizOfflineSaveRequest(List.of(
                new QuizOfflineSaveRequest.Row(studentId, new BigDecimal("4"), "Good"))));
        assertEquals(1, grid.rows().size());
        assertEquals(new BigDecimal("4"), grid.rows().get(0).marks());
    }

    @Test
    void createQuizPersistsItems() {
        stubLookups();
        QuizQuestion mcq = question(QuizQuestionType.MCQ, "B");
        when(questionRepository.findByIdAndSchoolId(mcq.getId(), TestSecurity.SCHOOL_ID)).thenReturn(Optional.of(mcq));
        when(quizRepository.findByIdAndSchoolId(any(), eq(TestSecurity.SCHOOL_ID)))
                .thenAnswer(inv -> Optional.ofNullable(quizzes.get(inv.getArgument(0))));
        when(itemRepository.findBySchoolIdAndQuizIdOrderBySortOrderAscCreatedAtAsc(eq(TestSecurity.SCHOOL_ID), any()))
                .thenAnswer(inv -> items.values().stream()
                        .filter(item -> inv.getArgument(1).equals(item.getQuizId())).toList());
        when(attemptRepository.findBySchoolIdAndQuizIdOrderByAttemptNoAsc(eq(TestSecurity.SCHOOL_ID), any()))
                .thenReturn(List.of());

        service.createQuiz(new QuizRequest(yearId, classId, sectionId, subjectId, "Warm-up", "Do all",
                20, 1, Instant.now(), Instant.now().plusSeconds(3600), new BigDecimal("3"),
                false, true, List.of(new QuizItemRequest(mcq.getId(), 1, new BigDecimal("2")))));

        verify(itemRepository).deleteByQuizIdAndSchoolId(any(), eq(TestSecurity.SCHOOL_ID));
        ArgumentCaptor<QuizItem> captor = ArgumentCaptor.forClass(QuizItem.class);
        verify(itemRepository).save(captor.capture());
        assertEquals(mcq.getId(), captor.getValue().getQuestionId());
        assertEquals(new BigDecimal("2"), captor.getValue().getMarks());
    }

    @Test
    void cannotDeleteQuestionInUse() {
        QuizQuestion question = question(QuizQuestionType.MCQ, "A");
        when(questionRepository.findByIdAndSchoolId(question.getId(), TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(question));
        when(itemRepository.existsByQuestionId(question.getId())).thenReturn(true);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.deleteQuestion(question.getId()));
        assertEquals("quiz.question_in_use", ex.getCode());
        verify(questionRepository, never()).delete(any());
    }

    private void stubLookups() {
        org.mockito.Mockito.lenient().when(academicYearRepository.findByIdAndSchoolId(yearId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(year()));
        org.mockito.Mockito.lenient().when(classRepository.findByIdAndSchoolId(classId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(schoolClass()));
        org.mockito.Mockito.lenient().when(sectionRepository.findByIdAndSchoolId(sectionId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(section()));
        org.mockito.Mockito.lenient().when(subjectRepository.findByIdAndSchoolId(subjectId, TestSecurity.SCHOOL_ID))
                .thenReturn(Optional.of(subject()));
        org.mockito.Mockito.lenient().when(academicYearRepository.findBySchoolIdOrderByStartDateDesc(TestSecurity.SCHOOL_ID))
                .thenReturn(List.of(year()));
        org.mockito.Mockito.lenient().when(classRepository.findBySchoolIdOrderBySortOrderAsc(TestSecurity.SCHOOL_ID))
                .thenReturn(List.of(schoolClass()));
        org.mockito.Mockito.lenient().when(sectionRepository.findBySchoolIdOrderByNameAsc(TestSecurity.SCHOOL_ID))
                .thenReturn(List.of(section()));
        org.mockito.Mockito.lenient().when(subjectRepository.findBySchoolIdOrderByNameAsc(TestSecurity.SCHOOL_ID))
                .thenReturn(List.of(subject()));
        org.mockito.Mockito.lenient().when(teacherRepository.findBySchoolIdAndUserId(TestSecurity.SCHOOL_ID, TestSecurity.USER_ID))
                .thenReturn(Optional.empty());
    }

    private AcademicYear year() {
        AcademicYear year = new AcademicYear();
        year.setId(yearId);
        year.setSchoolId(TestSecurity.SCHOOL_ID);
        year.setName("2025-26");
        year.setStartDate(LocalDate.of(2025, 4, 1));
        year.setEndDate(LocalDate.of(2026, 3, 31));
        year.setCurrent(true);
        return year;
    }

    private SchoolClass schoolClass() {
        SchoolClass klass = new SchoolClass();
        klass.setId(classId);
        klass.setSchoolId(TestSecurity.SCHOOL_ID);
        klass.setName("Class 5");
        return klass;
    }

    private Section section() {
        Section section = new Section();
        section.setId(sectionId);
        section.setSchoolId(TestSecurity.SCHOOL_ID);
        section.setClassId(classId);
        section.setName("A");
        return section;
    }

    private Subject subject() {
        Subject subject = new Subject();
        subject.setId(subjectId);
        subject.setSchoolId(TestSecurity.SCHOOL_ID);
        subject.setName("Mathematics");
        return subject;
    }

    private Student student() {
        Student student = new Student();
        student.setId(studentId);
        student.setSchoolId(TestSecurity.SCHOOL_ID);
        student.setAdmissionNo("ADM0001");
        student.setFirstName("Aarav");
        student.setLastName("Kumar");
        return student;
    }

    private StudentEnrollment enrollment() {
        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setId(UUID.randomUUID());
        enrollment.setSchoolId(TestSecurity.SCHOOL_ID);
        enrollment.setStudentId(studentId);
        enrollment.setSectionId(sectionId);
        enrollment.setAcademicYearId(yearId);
        enrollment.setRollNumber(1);
        enrollment.setStatus("ACTIVE");
        return enrollment;
    }

    private Quiz quiz(QuizStatus status) {
        Quiz quiz = new Quiz();
        quiz.setId(quizId);
        quiz.setSchoolId(TestSecurity.SCHOOL_ID);
        quiz.setAcademicYearId(yearId);
        quiz.setClassId(classId);
        quiz.setSectionId(sectionId);
        quiz.setSubjectId(subjectId);
        quiz.setTitle("Warm-up");
        quiz.setDurationMinutes(20);
        quiz.setMaxAttempts(2);
        quiz.setStatus(status);
        quiz.setShowResults(true);
        quiz.setPassMarks(new BigDecimal("3"));
        quizzes.put(quiz.getId(), quiz);
        return quiz;
    }

    private QuizQuestion question(QuizQuestionType type, String correct) {
        QuizQuestion question = new QuizQuestion();
        question.setId(UUID.randomUUID());
        question.setSchoolId(TestSecurity.SCHOOL_ID);
        question.setSubjectId(subjectId);
        question.setClassId(classId);
        question.setQuestionType(type);
        question.setPrompt("Q");
        question.setCorrectAnswer(correct);
        question.setMarks(BigDecimal.ONE);
        questions.put(question.getId(), question);
        return question;
    }

    private QuizItem item(UUID quiz, QuizQuestion question, int marks) {
        QuizItem item = new QuizItem();
        item.setId(UUID.randomUUID());
        item.setSchoolId(TestSecurity.SCHOOL_ID);
        item.setQuizId(quiz);
        item.setQuestionId(question.getId());
        item.setMarks(BigDecimal.valueOf(marks));
        items.put(item.getId(), item);
        return item;
    }

    private QuizAttempt attempt(Quiz quiz, QuizAttemptStatus status, Instant expiresAt) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setId(UUID.randomUUID());
        attempt.setSchoolId(TestSecurity.SCHOOL_ID);
        attempt.setQuizId(quiz.getId());
        attempt.setStudentId(studentId);
        attempt.setAttemptNo(1);
        attempt.setStatus(status);
        attempt.setStartedAt(Instant.now().minusSeconds(30));
        attempt.setExpiresAt(expiresAt);
        attempts.put(attempt.getId(), attempt);
        return attempt;
    }

    private static <T> Answer<T> persist(Map<UUID, T> store) {
        return invocation -> {
            T entity = invocation.getArgument(0);
            try {
                var getId = entity.getClass().getMethod("getId");
                var setId = entity.getClass().getMethod("setId", UUID.class);
                UUID id = (UUID) getId.invoke(entity);
                if (id == null) {
                    id = UUID.randomUUID();
                    setId.invoke(entity, id);
                }
                store.put(id, entity);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            return entity;
        };
    }
}
