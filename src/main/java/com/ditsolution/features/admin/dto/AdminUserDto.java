package com.ditsolution.features.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserDto(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String phoneE164,
    boolean phoneVerified,
    boolean emailVerified,
    String role,
    String status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String avatarUrl,
    long listingsCount
) {}
