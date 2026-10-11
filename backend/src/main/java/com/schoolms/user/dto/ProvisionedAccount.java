package com.schoolms.user.dto;

import java.util.UUID;

public record ProvisionedAccount(
        UUID userId,
        String username,
        String temporaryPassword
) {
}
