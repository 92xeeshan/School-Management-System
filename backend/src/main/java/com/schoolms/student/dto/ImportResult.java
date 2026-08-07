package com.schoolms.student.dto;

import java.util.List;

public record ImportResult(
        int total,
        int succeeded,
        List<String> errors
) {
}
