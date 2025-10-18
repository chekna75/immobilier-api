package com.ditsolution.features.admin.dto;

import java.util.UUID;

public record AdminImpersonateDto(
    UUID targetUserId,
    String reason
) {}
