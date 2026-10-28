package com.schoolms.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectLeaveRequest(
        @NotBlank @Size(max = 500) String rejectionReason
) {
}
