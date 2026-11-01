package com.schoolms.website.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateEnquiryRequest(
        @Pattern(regexp = "NEW|CONTACTED|CLOSED") String status,
        @Size(max = 1000) String notes
) {
}
