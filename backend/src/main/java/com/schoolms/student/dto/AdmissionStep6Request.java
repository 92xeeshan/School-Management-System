package com.schoolms.student.dto;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record AdmissionStep6Request(
        boolean transportRequired,
        boolean hostelRequired,
        @Size(max = 1000) String medicalNotes,
        @Size(max = 1000) String remarks,
        List<UUID> siblingIds
) {
}
