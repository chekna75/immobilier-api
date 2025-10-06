package com.ditsolution.features.admin.dto;

public record AdminUserFilterDto(
    String email,
    String role,
    String status,
    Integer page,
    Integer size,
    String sortBy,
    String sortDirection
) {
    public AdminUserFilterDto {
        if (page == null || page < 0) page = 0;
        if (size == null || size < 1) size = 20;
        if (size > 100) size = 100;
        if (sortBy == null || sortBy.isBlank()) sortBy = "createdAt";
        if (sortDirection == null || sortDirection.isBlank()) sortDirection = "desc";
    }
}
