package com.ditsolution.features.admin.dto;

import java.math.BigDecimal;

public record AdminDashboardDto(
    // KPIs utilisateurs
    long totalUsers,
    long activeUsers,
    long suspendedUsers,
    
    // KPIs annonces
    long totalListings,
    long activeListings,
    long removedListings,
    long reportedListings,
    
    // KPIs stockage
    long totalImages,
    BigDecimal storageUsedMB,
    BigDecimal storageUsedGB,
    
    // Données récentes
    long newUsersLast7Days,
    long newListingsLast7Days,
    long newImagesLast7Days
) {}