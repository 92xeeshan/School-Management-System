package com.schoolms.school.dto;

import com.schoolms.common.enums.SchoolStatus;
import com.schoolms.school.School;

import java.time.Instant;
import java.util.UUID;

public record SchoolDto(
        UUID id,
        String code,
        String name,
        String address,
        String phone,
        String email,
        String logoUrl,
        String currency,
        String defaultLocale,
        String timezone,
        Short academicStartMonth,
        SchoolStatus status,
        Instant createdAt
) {
    public static SchoolDto from(School school) {
        return new SchoolDto(
                school.getId(), school.getCode(), school.getName(), school.getAddress(),
                school.getPhone(), school.getEmail(), school.getLogoUrl(), school.getCurrency(),
                school.getDefaultLocale(), school.getTimezone(), school.getAcademicStartMonth(),
                school.getStatus(), school.getCreatedAt());
    }
}
