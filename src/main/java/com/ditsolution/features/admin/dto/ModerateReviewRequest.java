package com.ditsolution.features.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ModerateReviewRequest(
    @NotNull
    String action, // "APPROVE", "REJECT"
    
    @NotBlank
    String reason
) {
}
