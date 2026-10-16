package com.schoolms.student.dto;

import java.util.UUID;

/**
 * Criteria for querying the student directory. Any null field means "no
 * restriction". {@code sort} and {@code direction} are validated by the
 * service before being translated into a query order.
 */
public record StudentDirectoryFilter(
        UUID academicYearId,
        UUID classId,
        UUID sectionId,
        String status,
        String query,
        String sort,
        String direction
) {
    public static StudentDirectoryFilter empty() {
        return new StudentDirectoryFilter(null, null, null, null, null, null, null);
    }
}
