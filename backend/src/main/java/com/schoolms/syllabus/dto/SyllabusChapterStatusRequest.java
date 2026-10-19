package com.schoolms.syllabus.dto;

import com.schoolms.common.enums.SyllabusChapterStatus;
import jakarta.validation.constraints.NotNull;

public record SyllabusChapterStatusRequest(
        @NotNull(message = "{validation.not_null}") SyllabusChapterStatus status
) {
}
