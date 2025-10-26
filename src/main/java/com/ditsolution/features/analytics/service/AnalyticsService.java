package com.ditsolution.features.analytics.service;

import com.ditsolution.features.analytics.dto.*;
import com.ditsolution.features.analytics.entity.*;
import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.listing.entity.ListingEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AnalyticsService {

    private static final Logger LOG = Logger.getLogger(AnalyticsService.class);

    @Inject
    EntityManager entityManager;

    @Transactional
    public AnalyticsViewEntity trackView(AnalyticsViewDto dto) {
        try {
            AnalyticsViewEntity entity = new AnalyticsViewEntity();
            entity.listing = entityManager.getReference(ListingEntity.class, dto.listingId);
            if (dto.userId != null) {
                entity.user = entityManager.getReference(UserEntity.class, dto.userId);
            }
            entity.source = dto.source;
            entity.device = dto.device;
            entity.location = dto.location;
            entity.userAgent = dto.userAgent;
            entity.sessionId = dto.sessionId;
            entity.referrer = dto.referrer;
            entity.createdAt = dto.createdAt != null ? dto.createdAt : LocalDateTime.now();

            entityManager.persist(entity);
            LOG.info("Vue trackée pour l'annonce: " + dto.listingId);
            return entity;
        } catch (Exception e) {
            LOG.error("Erreur lors du tracking de la vue", e);
            throw new RuntimeException("Erreur lors du tracking de la vue", e);
        }
    }

    @Transactional
    public AnalyticsClickEntity trackClick(AnalyticsClickDto dto) {
        try {
            AnalyticsClickEntity entity = new AnalyticsClickEntity();
            entity.listing = entityManager.getReference(ListingEntity.class, dto.listingId);
            if (dto.userId != null) {
                entity.user = entityManager.getReference(UserEntity.class, dto.userId);
            }
            entity.action = dto.action;
            entity.source = dto.source;
            entity.device = dto.device;
            entity.location = dto.location;
            entity.sessionId = dto.sessionId;
            entity.createdAt = dto.createdAt != null ? dto.createdAt : LocalDateTime.now();

            entityManager.persist(entity);
            LOG.info("Clic tracké pour l'annonce: " + dto.listingId + ", action: " + dto.action);
            return entity;
        } catch (Exception e) {
            LOG.error("Erreur lors du tracking du clic", e);
            throw new RuntimeException("Erreur lors du tracking du clic", e);
        }
    }

    @Transactional
    public AnalyticsFavoriteEntity trackFavorite(AnalyticsFavoriteDto dto) {
        try {
            AnalyticsFavoriteEntity entity = new AnalyticsFavoriteEntity();
            entity.listing = entityManager.getReference(ListingEntity.class, dto.listingId);
            entity.user = entityManager.getReference(UserEntity.class, dto.userId);
            entity.action = dto.action;
            entity.source = dto.source;
            entity.device = dto.device;
            entity.createdAt = dto.createdAt != null ? dto.createdAt : LocalDateTime.now();

            entityManager.persist(entity);
            LOG.info("Favori tracké pour l'annonce: " + dto.listingId + ", action: " + dto.action);
            return entity;
        } catch (Exception e) {
            LOG.error("Erreur lors du tracking du favori", e);
            throw new RuntimeException("Erreur lors du tracking du favori", e);
        }
    }

    @Transactional
    public AnalyticsContactEntity trackContact(AnalyticsContactDto dto) {
        try {
            AnalyticsContactEntity entity = new AnalyticsContactEntity();
            entity.listing = entityManager.getReference(ListingEntity.class, dto.listingId);
            entity.user = entityManager.getReference(UserEntity.class, dto.userId);
            entity.contactType = dto.contactType;
            entity.source = dto.source;
            entity.device = dto.device;
            entity.message = dto.message;
            entity.createdAt = dto.createdAt != null ? dto.createdAt : LocalDateTime.now();

            entityManager.persist(entity);
            LOG.info("Contact tracké pour l'annonce: " + dto.listingId + ", type: " + dto.contactType);
            return entity;
        } catch (Exception e) {
            LOG.error("Erreur lors du tracking du contact", e);
            throw new RuntimeException("Erreur lors du tracking du contact", e);
        }
    }

    @Transactional
    public AnalyticsSearchEntity trackSearch(AnalyticsSearchDto dto) {
        try {
            AnalyticsSearchEntity entity = new AnalyticsSearchEntity();
            if (dto.userId != null) {
                entity.user = entityManager.getReference(UserEntity.class, dto.userId);
            }
            entity.query = dto.query;
            entity.filters = dto.filters;
            entity.source = dto.source;
            entity.device = dto.device;
            entity.location = dto.location;
            entity.resultsCount = dto.resultsCount;
            entity.createdAt = dto.createdAt != null ? dto.createdAt : LocalDateTime.now();

            entityManager.persist(entity);
            LOG.info("Recherche trackée: " + dto.query);
            return entity;
        } catch (Exception e) {
            LOG.error("Erreur lors du tracking de la recherche", e);
            throw new RuntimeException("Erreur lors du tracking de la recherche", e);
        }
    }

    @Transactional
    public AnalyticsConversionEntity trackConversion(AnalyticsConversionDto dto) {
        try {
            AnalyticsConversionEntity entity = new AnalyticsConversionEntity();
            entity.listing = entityManager.getReference(ListingEntity.class, dto.listingId);
            entity.user = entityManager.getReference(UserEntity.class, dto.userId);
            entity.conversionType = dto.conversionType;
            entity.source = dto.source;
            entity.device = dto.device;
            entity.value = dto.value;
            entity.status = dto.status;
            entity.createdAt = dto.createdAt != null ? dto.createdAt : LocalDateTime.now();

            entityManager.persist(entity);
            LOG.info("Conversion trackée pour l'annonce: " + dto.listingId + ", type: " + dto.conversionType);
            return entity;
        } catch (Exception e) {
            LOG.error("Erreur lors du tracking de la conversion", e);
            throw new RuntimeException("Erreur lors du tracking de la conversion", e);
        }
    }

    // Méthodes pour récupérer les statistiques
    public List<AnalyticsViewEntity> getViewsByListing(UUID listingId, LocalDateTime startDate, LocalDateTime endDate) {
        return entityManager.createQuery(
            "SELECT v FROM AnalyticsViewEntity v WHERE v.listing.id = :listingId " +
            "AND v.createdAt BETWEEN :startDate AND :endDate ORDER BY v.createdAt DESC",
            AnalyticsViewEntity.class)
            .setParameter("listingId", listingId)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
    }

    public List<AnalyticsClickEntity> getClicksByListing(UUID listingId, LocalDateTime startDate, LocalDateTime endDate) {
        return entityManager.createQuery(
            "SELECT c FROM AnalyticsClickEntity c WHERE c.listing.id = :listingId " +
            "AND c.createdAt BETWEEN :startDate AND :endDate ORDER BY c.createdAt DESC",
            AnalyticsClickEntity.class)
            .setParameter("listingId", listingId)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
    }

    public List<AnalyticsFavoriteEntity> getFavoritesByListing(UUID listingId, LocalDateTime startDate, LocalDateTime endDate) {
        return entityManager.createQuery(
            "SELECT f FROM AnalyticsFavoriteEntity f WHERE f.listing.id = :listingId " +
            "AND f.createdAt BETWEEN :startDate AND :endDate ORDER BY f.createdAt DESC",
            AnalyticsFavoriteEntity.class)
            .setParameter("listingId", listingId)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
    }

    public List<AnalyticsContactEntity> getContactsByListing(UUID listingId, LocalDateTime startDate, LocalDateTime endDate) {
        return entityManager.createQuery(
            "SELECT c FROM AnalyticsContactEntity c WHERE c.listing.id = :listingId " +
            "AND c.createdAt BETWEEN :startDate AND :endDate ORDER BY c.createdAt DESC",
            AnalyticsContactEntity.class)
            .setParameter("listingId", listingId)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
    }

    public List<AnalyticsConversionEntity> getConversionsByListing(UUID listingId, LocalDateTime startDate, LocalDateTime endDate) {
        return entityManager.createQuery(
            "SELECT c FROM AnalyticsConversionEntity c WHERE c.listing.id = :listingId " +
            "AND c.createdAt BETWEEN :startDate AND :endDate ORDER BY c.createdAt DESC",
            AnalyticsConversionEntity.class)
            .setParameter("listingId", listingId)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
    }
}
