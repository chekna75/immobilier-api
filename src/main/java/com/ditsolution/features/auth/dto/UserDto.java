package com.ditsolution.features.auth.dto;

import com.ditsolution.features.auth.entity.UserEntity;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserDto(
    UUID id,
    UserEntity.Role role,
    UserEntity.Status status,
    String email,
    String phoneE164,
    boolean phoneVerified,
    boolean emailVerified,
    String firstName,
    String lastName,
    String avatarUrl,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}


