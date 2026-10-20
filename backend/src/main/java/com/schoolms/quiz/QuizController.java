package com.schoolms.quiz;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.quiz.dto.QuizAttemptDto;
import com.schoolms.quiz.dto.QuizDto;
import com.schoolms.quiz.dto.QuizGradeRequest;
import com.schoolms.quiz.dto.QuizOfflineGridDto;
import com.schoolms.quiz.dto.QuizOfflineSaveRequest;
import com.schoolms.quiz.dto.QuizOptionsDto;
import com.schoolms.quiz.dto.QuizQuestionDto;
import com.schoolms.quiz.dto.QuizQuestionRequest;
import com.schoolms.quiz.dto.QuizRequest;
import com.schoolms.quiz.dto.QuizResultRowDto;
import com.schoolms.quiz.dto.QuizStatusRequest;
import com.schoolms.quiz.dto.QuizSubmitRequest;
import com.schoolms.quiz.dto.QuizTakeDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Quizzes")
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService quizService;

    @Operation(summary = "Filter options for quizzes")
    @PreAuthorize("hasAnyAuthority('QUIZ_READ', 'QUIZ_MANAGE')")
    @GetMapping("/options")
    public ApiResponse<QuizOptionsDto> options() {
        return ApiResponse.ok(quizService.options());
    }

    @Operation(summary = "List question bank")
    @PreAuthorize("hasAnyAuthority('QUIZ_READ', 'QUIZ_MANAGE')")
    @GetMapping("/questions")
    public ApiResponse<List<QuizQuestionDto>> listQuestions(
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) String questionType) {
        return ApiResponse.ok(quizService.listQuestions(subjectId, classId, questionType));
    }

    @Operation(summary = "Create a question")
    @PreAuthorize("hasAuthority('QUIZ_MANAGE')")
    @PostMapping("/questions")
    public ApiResponse<QuizQuestionDto> createQuestion(@Valid @RequestBody QuizQuestionRequest request) {
        return ApiResponse.ok(quizService.createQuestion(request), "quiz.question_created");
    }

    @Operation(summary = "Update a question")
    @PreAuthorize("hasAuthority('QUIZ_MANAGE')")
    @PutMapping("/questions/{id}")
    public ApiResponse<QuizQuestionDto> updateQuestion(@PathVariable UUID id,
                                                       @Valid @RequestBody QuizQuestionRequest request) {
        return ApiResponse.ok(quizService.updateQuestion(id, request), "quiz.question_updated");
    }

    @Operation(summary = "Delete a question")
    @PreAuthorize("hasAuthority('QUIZ_MANAGE')")
    @DeleteMapping("/questions/{id}")
    public ApiResponse<Void> deleteQuestion(@PathVariable UUID id) {
        quizService.deleteQuestion(id);
        return ApiResponse.okMessage("quiz.question_deleted");
    }

    @Operation(summary = "List quizzes")
    @PreAuthorize("hasAnyAuthority('QUIZ_READ', 'QUIZ_MANAGE')")
    @GetMapping
    public ApiResponse<List<QuizDto>> list(
            @RequestParam(required = false) UUID academicYearId,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID studentId) {
        return ApiResponse.ok(quizService.listQuizzes(
                academicYearId, classId, sectionId, subjectId, status, studentId));
    }

    @Operation(summary = "Get a quiz")
    @PreAuthorize("hasAnyAuthority('QUIZ_READ', 'QUIZ_MANAGE')")
    @GetMapping("/{id}")
    public ApiResponse<QuizDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(quizService.getQuiz(id));
    }

    @Operation(summary = "Create a quiz")
    @PreAuthorize("hasAuthority('QUIZ_MANAGE')")
    @PostMapping
    public ApiResponse<QuizDto> create(@Valid @RequestBody QuizRequest request) {
        return ApiResponse.ok(quizService.createQuiz(request), "quiz.created");
    }

    @Operation(summary = "Update a quiz")
    @PreAuthorize("hasAuthority('QUIZ_MANAGE')")
    @PutMapping("/{id}")
    public ApiResponse<QuizDto> update(@PathVariable UUID id, @Valid @RequestBody QuizRequest request) {
        return ApiResponse.ok(quizService.updateQuiz(id, request), "quiz.updated");
    }

    @Operation(summary = "Change quiz status")
    @PreAuthorize("hasAuthority('QUIZ_MANAGE')")
    @PutMapping("/{id}/status")
    public ApiResponse<QuizDto> updateStatus(@PathVariable UUID id,
                                             @Valid @RequestBody QuizStatusRequest request) {
        return ApiResponse.ok(quizService.updateStatus(id, request), "quiz.status_updated");
    }

    @Operation(summary = "Delete a quiz")
    @PreAuthorize("hasAuthority('QUIZ_MANAGE')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        quizService.deleteQuiz(id);
        return ApiResponse.okMessage("quiz.deleted");
    }

    @Operation(summary = "Start a quiz attempt")
    @PreAuthorize("hasAnyAuthority('QUIZ_READ', 'QUIZ_MANAGE')")
    @PostMapping("/{id}/attempts")
    public ApiResponse<QuizTakeDto> start(@PathVariable UUID id,
                                          @RequestParam(required = false) UUID studentId) {
        return ApiResponse.ok(quizService.startAttempt(id, studentId), "quiz.attempt_started");
    }

    @Operation(summary = "Save in-progress answers")
    @PreAuthorize("hasAnyAuthority('QUIZ_READ', 'QUIZ_MANAGE')")
    @PutMapping("/attempts/{attemptId}")
    public ApiResponse<QuizAttemptDto> saveProgress(@PathVariable UUID attemptId,
                                                    @Valid @RequestBody QuizSubmitRequest request) {
        return ApiResponse.ok(quizService.saveProgress(attemptId, request));
    }

    @Operation(summary = "Submit a quiz attempt")
    @PreAuthorize("hasAnyAuthority('QUIZ_READ', 'QUIZ_MANAGE')")
    @PostMapping("/attempts/{attemptId}/submit")
    public ApiResponse<QuizAttemptDto> submit(@PathVariable UUID attemptId,
                                              @Valid @RequestBody QuizSubmitRequest request) {
        return ApiResponse.ok(quizService.submitAttempt(attemptId, request), "quiz.attempt_submitted");
    }

    @Operation(summary = "Get an attempt")
    @PreAuthorize("hasAnyAuthority('QUIZ_READ', 'QUIZ_MANAGE')")
    @GetMapping("/attempts/{attemptId}")
    public ApiResponse<QuizAttemptDto> getAttempt(@PathVariable UUID attemptId) {
        return ApiResponse.ok(quizService.getAttempt(attemptId));
    }

    @Operation(summary = "List attempts for a quiz")
    @PreAuthorize("hasAuthority('QUIZ_MANAGE')")
    @GetMapping("/{id}/attempts")
    public ApiResponse<List<QuizAttemptDto>> listAttempts(@PathVariable UUID id) {
        return ApiResponse.ok(quizService.listAttempts(id));
    }

    @Operation(summary = "Grade short-answer questions")
    @PreAuthorize("hasAuthority('QUIZ_MANAGE')")
    @PostMapping("/attempts/{attemptId}/grade")
    public ApiResponse<QuizAttemptDto> grade(@PathVariable UUID attemptId,
                                             @Valid @RequestBody QuizGradeRequest request) {
        return ApiResponse.ok(quizService.gradeAttempt(attemptId, request), "quiz.attempt_graded");
    }

    @Operation(summary = "Offline marks grid")
    @PreAuthorize("hasAnyAuthority('QUIZ_READ', 'QUIZ_MANAGE')")
    @GetMapping("/{id}/offline")
    public ApiResponse<QuizOfflineGridDto> offlineGrid(@PathVariable UUID id) {
        return ApiResponse.ok(quizService.offlineGrid(id));
    }

    @Operation(summary = "Save offline marks")
    @PreAuthorize("hasAuthority('QUIZ_MANAGE')")
    @PostMapping("/{id}/offline")
    public ApiResponse<QuizOfflineGridDto> saveOffline(@PathVariable UUID id,
                                                       @Valid @RequestBody QuizOfflineSaveRequest request) {
        return ApiResponse.ok(quizService.saveOffline(id, request), "quiz.offline_saved");
    }

    @Operation(summary = "Quiz results")
    @PreAuthorize("hasAnyAuthority('QUIZ_READ', 'QUIZ_MANAGE')")
    @GetMapping("/{id}/results")
    public ApiResponse<List<QuizResultRowDto>> results(@PathVariable UUID id) {
        return ApiResponse.ok(quizService.results(id));
    }

    @Operation(summary = "Download quiz results PDF")
    @PreAuthorize("hasAnyAuthority('QUIZ_READ', 'QUIZ_MANAGE')")
    @GetMapping("/{id}/results/pdf")
    public ResponseEntity<byte[]> resultsPdf(@PathVariable UUID id) {
        byte[] body = quizService.exportResultsPdf(id);
        return ResponseEntity.ok()
                .headers(quizService.downloadHeaders("quiz-results.pdf", MediaType.APPLICATION_PDF_VALUE))
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, quizService.contentDisposition("quiz-results.pdf"))
                .body(body);
    }

    @Operation(summary = "Download a student attempt PDF")
    @PreAuthorize("hasAnyAuthority('QUIZ_READ', 'QUIZ_MANAGE')")
    @GetMapping("/attempts/{attemptId}/pdf")
    public ResponseEntity<byte[]> attemptPdf(@PathVariable UUID attemptId) {
        byte[] body = quizService.exportAttemptPdf(attemptId);
        return ResponseEntity.ok()
                .headers(quizService.downloadHeaders("quiz-result.pdf", MediaType.APPLICATION_PDF_VALUE))
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, quizService.contentDisposition("quiz-result.pdf"))
                .body(body);
    }
}
