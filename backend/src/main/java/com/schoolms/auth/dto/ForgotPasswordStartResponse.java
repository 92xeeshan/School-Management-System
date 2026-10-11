package com.schoolms.auth.dto;

import java.util.List;
import java.util.UUID;

public record ForgotPasswordStartResponse(
        UUID challengeId,
        List<SecurityQuestionDto> questions
) {
}
