package com.schoolms.website.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SubmitEnquiryRequest(
        @NotBlank @Size(max = 120) String parentName,
        @NotBlank @Size(max = 20) String phone,
        @Email @Size(max = 120) String email,
        UUID classId,
        @Size(max = 80) String className,
        @Size(max = 1000) String message
) {
}
