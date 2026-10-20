package com.schoolms.quiz;

import com.schoolms.common.BaseEntity;
import com.schoolms.common.enums.QuizAttemptStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempt")
@Getter
@Setter
public class QuizAttempt extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuizAttemptStatus status = QuizAttemptStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "auto_score")
    private BigDecimal autoScore;

    @Column(name = "manual_score")
    private BigDecimal manualScore;

    @Column(name = "total_score")
    private BigDecimal totalScore;

    @Column(name = "max_score")
    private BigDecimal maxScore;

    private BigDecimal percentage;
}
