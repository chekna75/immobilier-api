package com.ditsolution.features.admin.dto;

import java.math.BigDecimal;

public record AdminListingFilterDto(
    String city,
    String district,
    String type,
    String status,
    String ownerEmail,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    Integer page,
    Integer size,
    String sortBy,
    String sortDirection
) {
    public AdminListingFilterDto {
        if (page == null || page < 0) page = 0;
        if (size == null || size < 1) size = 20;
        if (size > 100) size = 100;
        if (sortBy == null || sortBy.isBlank()) sortBy = "createdAt";
        if (sortDirection == null || sortDirection.isBlank()) sortDirection = "desc";
    }
}
