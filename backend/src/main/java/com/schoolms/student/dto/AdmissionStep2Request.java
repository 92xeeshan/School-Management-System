package com.schoolms.student.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdmissionStep2Request(
        @Valid FamilyMemberDto father,
        @Valid FamilyMemberDto mother,
        @Valid FamilyMemberDto guardian,
        boolean guardianDifferent,
        @Size(max = 120) String emergencyName,
        @Size(max = 40) String emergencyRelation,
        @Pattern(regexp = "^$|^\\d{10}$", message = "validation.invalid") String emergencyPhone,
        @Size(max = 20) String primaryContact
) {
}
