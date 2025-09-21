package com.ditsolution.features.admin.service;

import com.ditsolution.features.admin.dto.AdminDashboardDto;
import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.listing.enums.ListingStatus;
import com.ditsolution.features.storage.entity.UploadedImageEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.math.RoundingMode;

@ApplicationScoped
public class AdminDashboardService {

    @PersistenceContext
    EntityManager em;

    public AdminDashboardDto getDashboardStats() {
        // Statistiques utilisateurs
        long activeUsers = UserEntity.count("status = ?1", UserEntity.Status.ACTIVE);
        long totalUsers = UserEntity.count();

        // Statistiques annonces
        long publishedListings = em.createQuery("SELECT COUNT(l) FROM ListingEntity l WHERE l.status = :status", Long.class)
            .setParameter("status", ListingStatus.PUBLISHED).getSingleResult();
        long draftListings = em.createQuery("SELECT COUNT(l) FROM ListingEntity l WHERE l.status = :status", Long.class)
            .setParameter("status", ListingStatus.DRAFT).getSingleResult();
        long removedListings = em.createQuery("SELECT COUNT(l) FROM ListingEntity l WHERE l.status = :status", Long.class)
            .setParameter("status", ListingStatus.REMOVED).getSingleResult();
        long totalListings = em.createQuery("SELECT COUNT(l) FROM ListingEntity l", Long.class).getSingleResult();

        // Statistiques stockage
        long totalImages = UploadedImageEntity.count();
        long usedImages = UploadedImageEntity.count("isUsed = ?1", true);
        long unusedImages = UploadedImageEntity.count("isUsed = ?1", false);

        // Calcul du stockage total utilisé (en MB)
        BigDecimal totalStorageUsed = calculateTotalStorageUsed();

        return new AdminDashboardDto(
            activeUsers,
            totalUsers,
            publishedListings,
            totalListings,
            draftListings,
            removedListings,
            totalStorageUsed,
            totalImages,
            usedImages,
            unusedImages
        );
    }

    private BigDecimal calculateTotalStorageUsed() {
        // Requête pour calculer la somme des tailles de fichiers
        String query = "SELECT COALESCE(SUM(fileSize), 0) FROM UploadedImageEntity";
        Long totalBytes = em.createQuery(query, Long.class).getSingleResult();
        
        if (totalBytes == null || totalBytes == 0) {
            return BigDecimal.ZERO;
        }
        
        // Conversion en MB avec 2 décimales
        BigDecimal totalMB = new BigDecimal(totalBytes)
            .divide(new BigDecimal(1024 * 1024), 2, RoundingMode.HALF_UP);
        
        return totalMB;
    }
}
