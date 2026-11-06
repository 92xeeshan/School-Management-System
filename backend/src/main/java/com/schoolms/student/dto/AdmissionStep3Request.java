package com.schoolms.student.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdmissionStep3Request(
        @Valid AddressDto currentAddress,
        @Valid AddressDto permanentAddress,
        boolean sameAsCurrent,
        @Pattern(regexp = "^$|^\\d{10}$", message = "validation.invalid") String phone,
        @Email @Size(max = 120) String email
) {
}
