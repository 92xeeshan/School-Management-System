package com.schoolms.quiz;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolms.common.exception.BusinessException;
import com.schoolms.quiz.dto.QuestionOptionDto;

import java.util.List;

public final class QuizOptionsJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<QuestionOptionDto>> TYPE = new TypeReference<>() {
    };

    private QuizOptionsJson() {
    }

    public static String toJson(List<QuestionOptionDto> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(options);
        } catch (Exception e) {
            throw new BusinessException("quiz.options_invalid");
        }
    }

    public static List<QuestionOptionDto> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<QuestionOptionDto> options = MAPPER.readValue(json, TYPE);
            return options == null ? List.of() : options;
        } catch (Exception e) {
            throw new BusinessException("quiz.options_invalid");
        }
    }
}
