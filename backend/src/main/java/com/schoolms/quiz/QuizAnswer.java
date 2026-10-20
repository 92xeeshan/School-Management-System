package com.schoolms.quiz;

import com.schoolms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "quiz_answer")
@Getter
@Setter
public class QuizAnswer extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(columnDefinition = "TEXT")
    private String response;

    @Column(name = "auto_correct")
    private Boolean autoCorrect;

    @Column(name = "auto_marks")
    private BigDecimal autoMarks;

    @Column(name = "awarded_marks")
    private BigDecimal awardedMarks;

    @Column(name = "grader_user_id")
    private UUID graderUserId;

    @Column(name = "grader_comment")
    private String graderComment;
}
