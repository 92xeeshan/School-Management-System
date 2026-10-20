package com.schoolms.quiz;

import com.schoolms.common.BaseEntity;
import com.schoolms.common.enums.QuizQuestionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "quiz_question")
@Getter
@Setter
public class QuizQuestion extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "class_id")
    private UUID classId;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuizQuestionType questionType = QuizQuestionType.MCQ;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "options_json", columnDefinition = "TEXT")
    private String optionsJson;

    @Column(name = "correct_answer")
    private String correctAnswer;

    @Column(nullable = false)
    private BigDecimal marks = BigDecimal.ONE;

    private String explanation;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;
}
