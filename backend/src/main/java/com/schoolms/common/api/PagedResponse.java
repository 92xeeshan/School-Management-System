package com.schoolms.common.api;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Simple pagination envelope decoupled from Spring Data types.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
