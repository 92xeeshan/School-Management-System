package com.schoolms.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Envelope for successful responses: {@code { success, message, data }}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> okMessage(String message) {
        return new ApiResponse<>(true, message, null);
    }
}
