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
        UUID userId,
        String username,
        String temporaryPassword
) {
    public static GuardianDto from(Guardian guardian) {
        return from(guardian, null, null);
    }

    public static GuardianDto from(Guardian guardian, String username, String temporaryPassword) {
        return new GuardianDto(
                guardian.getId(), guardian.getFirstName(), guardian.getLastName(),
                guardian.getDisplayName(), guardian.getRelationship().name(),
                guardian.getEmail(), guardian.getPhone(), guardian.getOccupation(),
                guardian.getUserId(), username, temporaryPassword);
    }
}
