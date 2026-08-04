package com.schoolms.student.dto;

import com.schoolms.student.Guardian;

import java.util.UUID;

public record GuardianDto(
        UUID id,
        String firstName,
        String lastName,
        String displayName,
        String relationship,
        String email,
        String phone,
        String occupation,
        UUID userId
) {
    public static GuardianDto from(Guardian guardian) {
        return new GuardianDto(
                guardian.getId(), guardian.getFirstName(), guardian.getLastName(),
                guardian.getDisplayName(), guardian.getRelationship().name(),
                guardian.getEmail(), guardian.getPhone(), guardian.getOccupation(),
                guardian.getUserId());
    }
}
