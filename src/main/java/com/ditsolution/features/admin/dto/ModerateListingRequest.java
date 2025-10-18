package com.ditsolution.features.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ModerateListingRequest(
    @NotNull
    String action, // "REMOVE", "RESTORE"
    
    @NotBlank
    String reason
) {
}
