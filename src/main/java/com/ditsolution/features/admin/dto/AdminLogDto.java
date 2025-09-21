package com.ditsolution.features.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminLogDto(
    UUID id,
    UUID adminId,
    String adminEmail,
    String action,
    String targetType,
    UUID targetId,
    String details,
    String ip,
    String userAgent,
    OffsetDateTime createdAt
) {
}
