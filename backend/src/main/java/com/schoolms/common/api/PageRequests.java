package com.schoolms.common.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PageRequests {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PageRequests() {
    }

    public static int clampPage(int page) {
        return Math.max(page, 0);
    }

    public static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }

    public static PageRequest of(int page, int size) {
        return PageRequest.of(clampPage(page), clampSize(size));
    }

    public static PageRequest of(int page, int size, Sort sort) {
        return PageRequest.of(clampPage(page), clampSize(size), sort == null ? Sort.unsorted() : sort);
    }

    public static PageRequest of(int page, int size, String sort, String direction,
                                 String defaultProperty, Map<String, String> allowed) {
        return of(page, size, sort(sort, direction, defaultProperty, allowed));
    }

    public static Sort sort(String sort, String direction, String defaultProperty, Map<String, String> allowed) {
        String key = sort == null ? "" : sort;
        String property = allowed == null ? defaultProperty : allowed.getOrDefault(key, defaultProperty);
        if (property == null || property.isBlank()) {
            return Sort.unsorted();
        }
        boolean desc = "desc".equalsIgnoreCase(direction);
        return Sort.by(new Sort.Order(desc ? Sort.Direction.DESC : Sort.Direction.ASC, property).nullsLast());
    }

    public static String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String likePattern(String query) {
        String normalized = normalizeQuery(query);
        return normalized == null ? null : "%" + normalized.toLowerCase(Locale.ROOT) + "%";
    }

    public static <T> PagedResponse<T> fromList(List<T> items, int page, int size) {
        List<T> source = items == null ? List.of() : items;
        int p = clampPage(page);
        int s = clampSize(size);
        int from = Math.min(p * s, source.size());
        int to = Math.min(from + s, source.size());
        int totalPages = s == 0 ? 0 : (int) Math.ceil(source.size() / (double) s);
        return new PagedResponse<>(source.subList(from, to), p, s, source.size(), totalPages);
    }
}
