package com.schoolms.exam.dto;

import java.util.List;

public record MarksImportResult(
        int total,
        int succeeded,
        List<String> errors,
        MarksGridDto grid
) {
}
