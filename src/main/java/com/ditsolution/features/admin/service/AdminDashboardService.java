package com.ditsolution.features.admin.service;

import com.ditsolution.features.admin.dto.AdminDashboardDto;
import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.storage.entity.UploadedImageEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;

@ApplicationScoped
public class AdminDashboardService {

    @Inject
    EntityManager entityManager;

    @Transactional
    public AdminDashboardDto getDashboardData() {
        // KPIs utilisateurs
        long totalUsers = UserEntity.count();
        long activeUsers = UserEntity.count("status", UserEntity.Status.ACTIVE);
        long suspendedUsers = UserEntity.count("status", UserEntity.Status.SUSPENDED);
        
        // KPIs annonces
        long totalListings = entityManager.createQuery("SELECT COUNT(l) FROM ListingEntity l", Long.class).getSingleResult();
        long activeListings = entityManager.createQuery("SELECT COUNT(l) FROM ListingEntity l WHERE l.status = 'ACTIVE'", Long.class).getSingleResult();
        long removedListings = entityManager.createQuery("SELECT COUNT(l) FROM ListingEntity l WHERE l.status = 'REMOVED'", Long.class).getSingleResult();
        long reportedListings = ((Number) entityManager.createNativeQuery("SELECT COUNT(DISTINCT r.target_id) FROM reviews r WHERE r.target_type = 'property' AND r.report_count > 0").getSingleResult()).longValue();
        
        // KPIs stockage
        long totalImages = UploadedImageEntity.count();
        BigDecimal storageUsedMB = getStorageUsedMB();
        BigDecimal storageUsedGB = storageUsedMB.divide(new BigDecimal("1024"), 2, java.math.RoundingMode.HALF_UP);
        
        // Données récentes (7 derniers jours) - Utilisation de requêtes natives pour éviter les problèmes de types
        long newUsersLast7Days = ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM users WHERE created_at >= NOW() - INTERVAL '7 days'").getSingleResult()).longValue();
        long newListingsLast7Days = ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM listings WHERE created_at >= NOW() - INTERVAL '7 days'").getSingleResult()).longValue();
        long newImagesLast7Days = ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM uploaded_images WHERE created_at >= NOW() - INTERVAL '7 days'").getSingleResult()).longValue();
        
        return new AdminDashboardDto(
            totalUsers,
            activeUsers,
            suspendedUsers,
            totalListings,
            activeListings,
            removedListings,
            reportedListings,
            totalImages,
            storageUsedMB,
            storageUsedGB,
            newUsersLast7Days,
            newListingsLast7Days,
            newImagesLast7Days
        );
    }
    
    private BigDecimal getStorageUsedMB() {
        // Calculer la taille totale des images stockées
        String query = "SELECT COALESCE(SUM(file_size), 0) FROM uploaded_images";
        Object result = entityManager.createNativeQuery(query).getSingleResult();
        BigDecimal totalBytes = (BigDecimal) result;
        return totalBytes.divide(new BigDecimal("1024").multiply(new BigDecimal("1024")), 2, java.math.RoundingMode.HALF_UP);
    }
}