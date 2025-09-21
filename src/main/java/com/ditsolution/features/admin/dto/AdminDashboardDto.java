package com.ditsolution.features.admin.dto;

import java.math.BigDecimal;

public record AdminDashboardDto(
    long activeUsers,
    long totalUsers,
    long publishedListings,
    long totalListings,
    long draftListings,
    long removedListings,
    BigDecimal totalStorageUsed,
    long totalImages,
    long usedImages,
    long unusedImages
) {
}
