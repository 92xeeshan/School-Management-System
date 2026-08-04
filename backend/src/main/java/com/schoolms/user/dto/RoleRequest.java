package com.schoolms.user.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleRequest(
        @NotBlank(message = "{validation.not_blank}") String roleCode
) {
}
