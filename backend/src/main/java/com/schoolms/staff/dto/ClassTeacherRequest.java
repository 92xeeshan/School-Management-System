package com.schoolms.staff.dto;

import java.util.UUID;

/**
 * Sets (or clears, when {@code sectionId} is null) the section for which a
 * teacher is the class teacher.
 */
public record ClassTeacherRequest(UUID sectionId) {
}
