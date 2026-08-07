package com.schoolms.common.api;

import java.time.Instant;
import java.util.List;

/**
 * Structured, localized error body returned by the global exception handler.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String code, String message) {
    }
}
