package com.ditsolution.features.admin.dto;

import java.util.UUID;

public record AdminUserUpdateDto(
    UUID userId,
    String role,
    String status,
    String reason
) {}
