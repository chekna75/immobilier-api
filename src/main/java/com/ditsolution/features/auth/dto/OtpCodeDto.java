package com.ditsolution.features.auth.dto;

import com.ditsolution.features.auth.entity.OtpCodeEntity;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OtpCodeDto(
    UUID id,
    UUID userId,
    String code,
    OtpCodeEntity.Channel channel,
    OtpCodeEntity.Purpose purpose,
    OffsetDateTime expiresAt,
    OffsetDateTime usedAt,
    int attemptCount,
    String meta,
    OffsetDateTime createdAt
) {}


