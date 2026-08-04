package com.schoolms.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocaleRequest(
        @NotBlank(message = "{validation.not_blank}") @Size(max = 10) String locale
) {
}
