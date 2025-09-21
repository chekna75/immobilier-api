package com.ditsolution.features.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;

public record AdminLogDto(
    UUID id,
    UUID adminId,
    String adminEmail,
    String action,
    String targetType,
    UUID targetId,
    JsonNode details,
    String ip,
    String userAgent,
    OffsetDateTime createdAt
) {
}
