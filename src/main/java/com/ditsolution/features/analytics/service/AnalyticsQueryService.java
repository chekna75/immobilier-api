package com.ditsolution.features.analytics.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class AnalyticsQueryService {

    private static final Logger LOG = Logger.getLogger(AnalyticsQueryService.class);

    @Inject
    EntityManager entityManager;

    /**
     * Récupère les statistiques en temps réel pour une annonce
     */
    public Map<String, Object> getListingRealTimeStats(UUID listingId, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            // Vues totales
            Long totalViews = entityManager.createQuery(
                "SELECT COUNT(v) FROM AnalyticsViewEntity v WHERE v.listing.id = :listingId " +
                "AND v.createdAt BETWEEN :startDate AND :endDate", Long.class)
                .setParameter("listingId", listingId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getSingleResult();

            // Vues uniques
            Long uniqueViews = entityManager.createQuery(
                "SELECT COUNT(DISTINCT v.user.id) FROM AnalyticsViewEntity v WHERE v.listing.id = :listingId " +
                "AND v.createdAt BETWEEN :startDate AND :endDate AND v.user IS NOT NULL", Long.class)
                .setParameter("listingId", listingId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getSingleResult();

            // Clics totaux
            Long totalClicks = entityManager.createQuery(
                "SELECT COUNT(c) FROM AnalyticsClickEntity c WHERE c.listing.id = :listingId " +
                "AND c.createdAt BETWEEN :startDate AND :endDate", Long.class)
                .setParameter("listingId", listingId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getSingleResult();

            // Favoris ajoutés
            Long totalFavorites = entityManager.createQuery(
                "SELECT COUNT(f) FROM AnalyticsFavoriteEntity f WHERE f.listing.id = :listingId " +
                "AND f.action = 'ADD' AND f.createdAt BETWEEN :startDate AND :endDate", Long.class)
                .setParameter("listingId", listingId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getSingleResult();

            // Contacts totaux
            Long totalContacts = entityManager.createQuery(
                "SELECT COUNT(c) FROM AnalyticsContactEntity c WHERE c.listing.id = :listingId " +
                "AND c.createdAt BETWEEN :startDate AND :endDate", Long.class)
                .setParameter("listingId", listingId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getSingleResult();

            // Conversions totales
            Long totalConversions = entityManager.createQuery(
                "SELECT COUNT(c) FROM AnalyticsConversionEntity c WHERE c.listing.id = :listingId " +
                "AND c.createdAt BETWEEN :startDate AND :endDate", Long.class)
                .setParameter("listingId", listingId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getSingleResult();

            // Calcul des taux
            BigDecimal conversionRate = totalViews > 0 ? 
                BigDecimal.valueOf(totalConversions).divide(BigDecimal.valueOf(totalViews), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

            BigDecimal clickThroughRate = totalViews > 0 ? 
                BigDecimal.valueOf(totalClicks).divide(BigDecimal.valueOf(totalViews), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

            BigDecimal favoriteRate = totalViews > 0 ? 
                BigDecimal.valueOf(totalFavorites).divide(BigDecimal.valueOf(totalViews), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

            BigDecimal contactRate = totalViews > 0 ? 
                BigDecimal.valueOf(totalContacts).divide(BigDecimal.valueOf(totalViews), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("totalViews", totalViews);
            result.put("uniqueViews", uniqueViews);
            result.put("totalClicks", totalClicks);
            result.put("totalFavorites", totalFavorites);
            result.put("totalContacts", totalContacts);
            result.put("totalConversions", totalConversions);
            result.put("conversionRate", conversionRate.doubleValue());
            result.put("clickThroughRate", clickThroughRate.doubleValue());
            result.put("favoriteRate", favoriteRate.doubleValue());
            result.put("contactRate", contactRate.doubleValue());
            return result;
        } catch (Exception e) {
            LOG.error("Erreur lors du calcul des statistiques en temps réel", e);
            throw new RuntimeException("Erreur lors du calcul des statistiques", e);
        }
    }

    /**
     * Récupère les statistiques en temps réel pour un propriétaire
     */
    public Map<String, Object> getOwnerRealTimeStats(UUID ownerId, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            // Récupérer les annonces du propriétaire
            List<UUID> listingIds = entityManager.createQuery(
                "SELECT l.id FROM ListingEntity l WHERE l.owner.id = :ownerId", UUID.class)
                .setParameter("ownerId", ownerId)
                .getResultList();

            if (listingIds.isEmpty()) {
                Map<String, Object> emptyResult = new java.util.HashMap<>();
                emptyResult.put("totalViews", 0L);
                emptyResult.put("uniqueViews", 0L);
                emptyResult.put("totalClicks", 0L);
                emptyResult.put("totalFavorites", 0L);
                emptyResult.put("totalContacts", 0L);
                emptyResult.put("totalConversions", 0L);
                emptyResult.put("totalListings", 0L);
                emptyResult.put("conversionRate", 0.0);
                emptyResult.put("clickThroughRate", 0.0);
                emptyResult.put("favoriteRate", 0.0);
                emptyResult.put("contactRate", 0.0);
                return emptyResult;
            }

            // Vues totales
            Long totalViews = entityManager.createQuery(
                "SELECT COUNT(v) FROM AnalyticsViewEntity v WHERE v.listing.id IN :listingIds " +
                "AND v.createdAt BETWEEN :startDate AND :endDate", Long.class)
                .setParameter("listingIds", listingIds)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getSingleResult();

            // Vues uniques
            Long uniqueViews = entityManager.createQuery(
                "SELECT COUNT(DISTINCT v.user.id) FROM AnalyticsViewEntity v WHERE v.listing.id IN :listingIds " +
                "AND v.createdAt BETWEEN :startDate AND :endDate AND v.user IS NOT NULL", Long.class)
                .setParameter("listingIds", listingIds)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getSingleResult();

            // Clics totaux
            Long totalClicks = entityManager.createQuery(
                "SELECT COUNT(c) FROM AnalyticsClickEntity c WHERE c.listing.id IN :listingIds " +
                "AND c.createdAt BETWEEN :startDate AND :endDate", Long.class)
                .setParameter("listingIds", listingIds)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getSingleResult();

            // Favoris ajoutés
            Long totalFavorites = entityManager.createQuery(
                "SELECT COUNT(f) FROM AnalyticsFavoriteEntity f WHERE f.listing.id IN :listingIds " +
                "AND f.action = 'ADD' AND f.createdAt BETWEEN :startDate AND :endDate", Long.class)
                .setParameter("listingIds", listingIds)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getSingleResult();

            // Contacts totaux
            Long totalContacts = entityManager.createQuery(
                "SELECT COUNT(c) FROM AnalyticsContactEntity c WHERE c.listing.id IN :listingIds " +
                "AND c.createdAt BETWEEN :startDate AND :endDate", Long.class)
                .setParameter("listingIds", listingIds)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getSingleResult();

            // Conversions totales
            Long totalConversions = entityManager.createQuery(
                "SELECT COUNT(c) FROM AnalyticsConversionEntity c WHERE c.listing.id IN :listingIds " +
                "AND c.createdAt BETWEEN :startDate AND :endDate", Long.class)
                .setParameter("listingIds", listingIds)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getSingleResult();

            // Calcul des taux
            BigDecimal conversionRate = totalViews > 0 ? 
                BigDecimal.valueOf(totalConversions).divide(BigDecimal.valueOf(totalViews), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

            BigDecimal clickThroughRate = totalViews > 0 ? 
                BigDecimal.valueOf(totalClicks).divide(BigDecimal.valueOf(totalViews), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

            BigDecimal favoriteRate = totalViews > 0 ? 
                BigDecimal.valueOf(totalFavorites).divide(BigDecimal.valueOf(totalViews), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

            BigDecimal contactRate = totalViews > 0 ? 
                BigDecimal.valueOf(totalContacts).divide(BigDecimal.valueOf(totalViews), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("totalViews", totalViews);
            result.put("uniqueViews", uniqueViews);
            result.put("totalClicks", totalClicks);
            result.put("totalFavorites", totalFavorites);
            result.put("totalContacts", totalContacts);
            result.put("totalConversions", totalConversions);
            result.put("totalListings", (long) listingIds.size());
            result.put("conversionRate", conversionRate.doubleValue());
            result.put("clickThroughRate", clickThroughRate.doubleValue());
            result.put("favoriteRate", favoriteRate.doubleValue());
            result.put("contactRate", contactRate.doubleValue());
            return result;
        } catch (Exception e) {
            LOG.error("Erreur lors du calcul des statistiques en temps réel du propriétaire", e);
            throw new RuntimeException("Erreur lors du calcul des statistiques", e);
        }
    }

    /**
     * Récupère les top annonces par vues
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTopListingsByViews(UUID ownerId, LocalDateTime startDate, LocalDateTime endDate, int limit) {
        try {
            return (List<Map<String, Object>>) entityManager.createNativeQuery(
                "SELECT l.id, l.title, COUNT(v.id) as view_count " +
                "FROM listings l " +
                "LEFT JOIN analytics_views v ON l.id = v.listing_id " +
                "WHERE l.owner_id = :ownerId " +
                "AND (v.created_at IS NULL OR v.created_at BETWEEN :startDate AND :endDate) " +
                "GROUP BY l.id, l.title " +
                "ORDER BY view_count DESC " +
                "LIMIT :limit")
                .setParameter("ownerId", ownerId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .setParameter("limit", limit)
                .getResultList();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des top annonces", e);
            throw new RuntimeException("Erreur lors de la récupération des top annonces", e);
        }
    }

    /**
     * Récupère les statistiques par source
     */
    public Map<String, Long> getStatsBySource(UUID listingId, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            List<Object[]> results = entityManager.createQuery(
                "SELECT v.source, COUNT(v) FROM AnalyticsViewEntity v " +
                "WHERE v.listing.id = :listingId AND v.createdAt BETWEEN :startDate AND :endDate " +
                "GROUP BY v.source", Object[].class)
                .setParameter("listingId", listingId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultList();

            return results.stream()
                .collect(java.util.stream.Collectors.toMap(
                    row -> (String) row[0],
                    row -> (Long) row[1]
                ));
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des statistiques par source", e);
            throw new RuntimeException("Erreur lors de la récupération des statistiques par source", e);
        }
    }

    /**
     * Récupère les statistiques par device
     */
    public Map<String, Long> getStatsByDevice(UUID listingId, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            List<Object[]> results = entityManager.createQuery(
                "SELECT v.device, COUNT(v) FROM AnalyticsViewEntity v " +
                "WHERE v.listing.id = :listingId AND v.createdAt BETWEEN :startDate AND :endDate " +
                "GROUP BY v.device", Object[].class)
                .setParameter("listingId", listingId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultList();

            return results.stream()
                .collect(java.util.stream.Collectors.toMap(
                    row -> (String) row[0],
                    row -> (Long) row[1]
                ));
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des statistiques par device", e);
            throw new RuntimeException("Erreur lors de la récupération des statistiques par device", e);
        }
    }

    /**
     * Récupère les statistiques par période (séries temporelles)
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTimeSeriesStats(UUID listingId, LocalDateTime startDate, LocalDateTime endDate, String granularity) {
        try {
            String dateFormat = granularity.equals("day") ? "yyyy-MM-dd" : 
                              granularity.equals("week") ? "yyyy-'W'ww" : "yyyy-MM";
            
            return (List<Map<String, Object>>) entityManager.createNativeQuery(
                "SELECT " +
                "TO_CHAR(v.created_at, :dateFormat) as period, " +
                "COUNT(v.id) as views, " +
                "COUNT(DISTINCT v.user_id) as unique_views, " +
                "COUNT(c.id) as clicks, " +
                "COUNT(f.id) as favorites, " +
                "COUNT(ct.id) as contacts " +
                "FROM analytics_views v " +
                "LEFT JOIN analytics_clicks c ON v.listing_id = c.listing_id AND TO_CHAR(c.created_at, :dateFormat) = TO_CHAR(v.created_at, :dateFormat) " +
                "LEFT JOIN analytics_favorites f ON v.listing_id = f.listing_id AND TO_CHAR(f.created_at, :dateFormat) = TO_CHAR(v.created_at, :dateFormat) AND f.action = 'ADD' " +
                "LEFT JOIN analytics_contacts ct ON v.listing_id = ct.listing_id AND TO_CHAR(ct.created_at, :dateFormat) = TO_CHAR(v.created_at, :dateFormat) " +
                "WHERE v.listing_id = :listingId " +
                "AND v.created_at BETWEEN :startDate AND :endDate " +
                "GROUP BY TO_CHAR(v.created_at, :dateFormat) " +
                "ORDER BY period")
                .setParameter("listingId", listingId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .setParameter("dateFormat", dateFormat)
                .getResultList();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des séries temporelles", e);
            throw new RuntimeException("Erreur lors de la récupération des séries temporelles", e);
        }
    }
}
