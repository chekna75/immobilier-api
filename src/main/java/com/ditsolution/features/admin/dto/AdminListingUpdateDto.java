package com.ditsolution.features.admin.dto;

import java.util.List;
import java.util.UUID;

public record AdminListingUpdateDto(
    List<UUID> listingIds,
    String status,
    String reason
) {}
