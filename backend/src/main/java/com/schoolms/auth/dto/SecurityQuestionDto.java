package com.schoolms.auth.dto;

import java.util.UUID;

public record SecurityQuestionDto(
        UUID id,
        String code
) {
}
