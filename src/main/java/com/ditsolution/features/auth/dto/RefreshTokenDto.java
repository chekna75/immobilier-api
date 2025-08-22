package com.ditsolution.features.auth.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RefreshTokenDto(
    UUID id,
    UUID userId,
    String tokenHash,
    String userAgent,
    String ipAddr,
    OffsetDateTime createdAt,
    OffsetDateTime expiresAt,
    OffsetDateTime revokedAt
) {}


