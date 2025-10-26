package com.ditsolution.features.analytics.service;

import com.ditsolution.features.analytics.dto.*;
import com.ditsolution.features.analytics.entity.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class AnalyticsReportService {

    private static final Logger LOG = Logger.getLogger(AnalyticsReportService.class);

    @Inject
    EntityManager entityManager;

    public AnalyticsReportDto generateListingReport(UUID listingId, String period) {
        try {
            LocalDateTime[] dateRange = getDateRange(period);
            LocalDateTime startDate = dateRange[0];
            LocalDateTime endDate = dateRange[1];

            // Récupérer les données
            List<AnalyticsViewEntity> views = getViewsByListing(listingId, startDate, endDate);
            List<AnalyticsClickEntity> clicks = getClicksByListing(listingId, startDate, endDate);
            List<AnalyticsFavoriteEntity> favorites = getFavoritesByListing(listingId, startDate, endDate);
            List<AnalyticsContactEntity> contacts = getContactsByListing(listingId, startDate, endDate);
            List<AnalyticsConversionEntity> conversions = getConversionsByListing(listingId, startDate, endDate);

            // Calculer les métriques
            AnalyticsMetricsDto metrics = calculateMetrics(views, clicks, favorites, contacts, conversions);
            
            // Calculer la répartition
            AnalyticsBreakdownDto breakdown = calculateBreakdown(views, clicks, contacts);
            
            // Calculer les séries temporelles
            AnalyticsTimeSeriesDto timeSeries = calculateTimeSeries(views, clicks, favorites, contacts, conversions, period);
            
            // Générer les recommandations
            List<AnalyticsRecommendationDto> recommendations = generateRecommendations(metrics);
            
            // Générer le résumé
            AnalyticsSummaryDto summary = generateSummary(metrics, period);
            
            // Générer les graphiques
            Map<String, Object> charts = generateCharts(timeSeries, breakdown);

            AnalyticsReportDto report = new AnalyticsReportDto();
            report.id = UUID.randomUUID();
            report.listingId = listingId;
            report.period = period;
            report.reportType = "listing";
            report.generatedAt = LocalDateTime.now();
            report.metrics = metrics;
            report.breakdown = breakdown;
            report.timeSeries = timeSeries;
            report.recommendations = recommendations;
            report.summary = summary;
            report.charts = charts;

            return report;
        } catch (Exception e) {
            LOG.error("Erreur lors de la génération du rapport d'annonce", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }

    public AnalyticsReportDto generateOwnerReport(UUID ownerId, String period) {
        try {
            LocalDateTime[] dateRange = getDateRange(period);
            LocalDateTime startDate = dateRange[0];
            LocalDateTime endDate = dateRange[1];

            // Récupérer les annonces du propriétaire
            List<UUID> listingIds = getOwnerListings(ownerId);
            
            if (listingIds.isEmpty()) {
                return createEmptyReport(ownerId, period);
            }

            // Récupérer les données pour toutes les annonces
            List<AnalyticsViewEntity> views = getViewsByListings(listingIds, startDate, endDate);
            List<AnalyticsClickEntity> clicks = getClicksByListings(listingIds, startDate, endDate);
            List<AnalyticsFavoriteEntity> favorites = getFavoritesByListings(listingIds, startDate, endDate);
            List<AnalyticsContactEntity> contacts = getContactsByListings(listingIds, startDate, endDate);
            List<AnalyticsConversionEntity> conversions = getConversionsByListings(listingIds, startDate, endDate);

            // Calculer les métriques
            AnalyticsMetricsDto metrics = calculateMetrics(views, clicks, favorites, contacts, conversions);
            
            // Calculer la répartition
            AnalyticsBreakdownDto breakdown = calculateBreakdown(views, clicks, contacts);
            
            // Calculer les séries temporelles
            AnalyticsTimeSeriesDto timeSeries = calculateTimeSeries(views, clicks, favorites, contacts, conversions, period);
            
            // Générer les recommandations
            List<AnalyticsRecommendationDto> recommendations = generateRecommendations(metrics);
            
            // Générer le résumé
            AnalyticsSummaryDto summary = generateSummary(metrics, period);
            
            // Générer les graphiques
            Map<String, Object> charts = generateCharts(timeSeries, breakdown);

            AnalyticsReportDto report = new AnalyticsReportDto();
            report.id = UUID.randomUUID();
            report.ownerId = ownerId;
            report.period = period;
            report.reportType = "owner";
            report.generatedAt = LocalDateTime.now();
            report.metrics = metrics;
            report.breakdown = breakdown;
            report.timeSeries = timeSeries;
            report.recommendations = recommendations;
            report.summary = summary;
            report.charts = charts;

            return report;
        } catch (Exception e) {
            LOG.error("Erreur lors de la génération du rapport de propriétaire", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }

    private LocalDateTime[] getDateRange(String period) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate;
        
        switch (period.toLowerCase()) {
            case "7d":
                startDate = endDate.minusDays(7);
                break;
            case "30d":
                startDate = endDate.minusDays(30);
                break;
            case "90d":
                startDate = endDate.minusDays(90);
                break;
            case "1y":
                startDate = endDate.minusYears(1);
                break;
            default:
                startDate = endDate.minusDays(30);
        }
        
        return new LocalDateTime[]{startDate, endDate};
    }

    private AnalyticsMetricsDto calculateMetrics(List<AnalyticsViewEntity> views, 
                                               List<AnalyticsClickEntity> clicks,
                                               List<AnalyticsFavoriteEntity> favorites,
                                               List<AnalyticsContactEntity> contacts,
                                               List<AnalyticsConversionEntity> conversions) {
        AnalyticsMetricsDto metrics = new AnalyticsMetricsDto();
        
        metrics.totalViews = (long) views.size();
        metrics.uniqueViews = views.stream()
            .map(v -> v.user != null ? v.user.id : null)
            .filter(Objects::nonNull)
            .distinct()
            .count();
        metrics.totalClicks = (long) clicks.size();
        metrics.totalFavorites = favorites.stream()
            .filter(f -> f.action == AnalyticsFavoriteEntity.FavoriteAction.ADD)
            .count();
        metrics.totalContacts = (long) contacts.size();
        metrics.totalConversions = (long) conversions.size();
        
        // Calculer les taux
        if (metrics.totalViews > 0) {
            metrics.conversionRate = BigDecimal.valueOf(metrics.totalConversions)
                .divide(BigDecimal.valueOf(metrics.totalViews), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
            metrics.clickThroughRate = BigDecimal.valueOf(metrics.totalClicks)
                .divide(BigDecimal.valueOf(metrics.totalViews), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
            metrics.favoriteRate = BigDecimal.valueOf(metrics.totalFavorites)
                .divide(BigDecimal.valueOf(metrics.totalViews), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
            metrics.contactRate = BigDecimal.valueOf(metrics.totalContacts)
                .divide(BigDecimal.valueOf(metrics.totalViews), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
        } else {
            metrics.conversionRate = 0.0;
            metrics.clickThroughRate = 0.0;
            metrics.favoriteRate = 0.0;
            metrics.contactRate = 0.0;
        }
        
        return metrics;
    }

    private AnalyticsBreakdownDto calculateBreakdown(List<AnalyticsViewEntity> views,
                                                   List<AnalyticsClickEntity> clicks,
                                                   List<AnalyticsContactEntity> contacts) {
        AnalyticsBreakdownDto breakdown = new AnalyticsBreakdownDto();
        
        // Répartition par source
        breakdown.viewsBySource = views.stream()
            .collect(Collectors.groupingBy(
                v -> v.source != null ? v.source : "unknown",
                Collectors.counting()
            ));
        
        // Répartition par device
        breakdown.viewsByDevice = views.stream()
            .collect(Collectors.groupingBy(
                v -> v.device != null ? v.device : "unknown",
                Collectors.counting()
            ));
        
        // Répartition des clics par action
        breakdown.clicksByAction = clicks.stream()
            .collect(Collectors.groupingBy(
                c -> c.action.toString(),
                Collectors.counting()
            ));
        
        // Répartition des contacts par type
        breakdown.contactsByType = contacts.stream()
            .collect(Collectors.groupingBy(
                c -> c.contactType.toString(),
                Collectors.counting()
            ));
        
        return breakdown;
    }

    private AnalyticsTimeSeriesDto calculateTimeSeries(List<AnalyticsViewEntity> views,
                                                      List<AnalyticsClickEntity> clicks,
                                                      List<AnalyticsFavoriteEntity> favorites,
                                                      List<AnalyticsContactEntity> contacts,
                                                      List<AnalyticsConversionEntity> conversions,
                                                      String period) {
        AnalyticsTimeSeriesDto timeSeries = new AnalyticsTimeSeriesDto();
        
        // Calculer les points de données pour chaque métrique
        timeSeries.views = calculateDataPoints(views, period);
        timeSeries.clicks = calculateDataPoints(clicks, period);
        timeSeries.favorites = calculateDataPoints(favorites, period);
        timeSeries.contacts = calculateDataPoints(contacts, period);
        timeSeries.conversions = calculateDataPoints(conversions, period);
        
        return timeSeries;
    }

    private List<AnalyticsDataPointDto> calculateDataPoints(List<?> entities, String period) {
        // Logique simplifiée - dans une vraie implémentation, 
        // on grouperait par jour/semaine/mois selon la période
        Map<String, Long> groupedData = entities.stream()
            .collect(Collectors.groupingBy(
                entity -> {
                    LocalDateTime date = getEntityDate(entity);
                    return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                },
                Collectors.counting()
            ));
        
        return groupedData.entrySet().stream()
            .map(entry -> {
                    AnalyticsDataPointDto point = new AnalyticsDataPointDto();
                    point.date = entry.getKey();
                    point.count = entry.getValue();
                    return point;
                })
            .sorted(Comparator.comparing(p -> p.date))
            .collect(Collectors.toList());
    }

    private LocalDateTime getEntityDate(Object entity) {
        if (entity instanceof AnalyticsViewEntity) {
            return ((AnalyticsViewEntity) entity).createdAt;
        } else if (entity instanceof AnalyticsClickEntity) {
            return ((AnalyticsClickEntity) entity).createdAt;
        } else if (entity instanceof AnalyticsFavoriteEntity) {
            return ((AnalyticsFavoriteEntity) entity).createdAt;
        } else if (entity instanceof AnalyticsContactEntity) {
            return ((AnalyticsContactEntity) entity).createdAt;
        } else if (entity instanceof AnalyticsConversionEntity) {
            return ((AnalyticsConversionEntity) entity).createdAt;
        }
        return LocalDateTime.now();
    }

    private List<AnalyticsRecommendationDto> generateRecommendations(AnalyticsMetricsDto metrics) {
        List<AnalyticsRecommendationDto> recommendations = new ArrayList<>();
        
        if (metrics.conversionRate < 5.0) {
            AnalyticsRecommendationDto rec = new AnalyticsRecommendationDto();
            rec.type = "improvement";
            rec.title = "Améliorer le taux de conversion";
            rec.description = "Votre taux de conversion est faible. Considérez améliorer les photos et descriptions.";
            rec.priority = "high";
            recommendations.add(rec);
        }
        
        if (metrics.totalViews < 100) {
            AnalyticsRecommendationDto rec = new AnalyticsRecommendationDto();
            rec.type = "visibility";
            rec.title = "Augmenter la visibilité";
            rec.description = "Vos annonces ont peu de vues. Essayez d'optimiser les mots-clés.";
            rec.priority = "medium";
            recommendations.add(rec);
        }
        
        return recommendations;
    }

    private AnalyticsSummaryDto generateSummary(AnalyticsMetricsDto metrics, String period) {
        AnalyticsSummaryDto summary = new AnalyticsSummaryDto();
        
        summary.overview = String.format(
            "Vos annonces ont généré %d vues avec un taux de conversion de %.2f%%",
            metrics.totalViews, metrics.conversionRate
        );
        
        summary.highlights = Arrays.asList(
            String.format("%d contacts générés", metrics.totalContacts),
            String.format("%d ajouts aux favoris", metrics.totalFavorites),
            String.format("%d visiteurs uniques", metrics.uniqueViews)
        );
        
        summary.trends = "Tendance stable"; // À implémenter avec une vraie analyse
        
        return summary;
    }

    private Map<String, Object> generateCharts(AnalyticsTimeSeriesDto timeSeries, AnalyticsBreakdownDto breakdown) {
        Map<String, Object> charts = new HashMap<>();
        charts.put("viewsOverTime", timeSeries.views);
        charts.put("contactsOverTime", timeSeries.contacts);
        charts.put("sourceBreakdown", breakdown.viewsBySource);
        charts.put("deviceBreakdown", breakdown.viewsByDevice);
        return charts;
    }

    // Méthodes de récupération des données (à implémenter avec de vraies requêtes)
    private List<AnalyticsViewEntity> getViewsByListing(UUID listingId, LocalDateTime startDate, LocalDateTime endDate) {
        return entityManager.createQuery(
            "SELECT v FROM AnalyticsViewEntity v WHERE v.listing.id = :listingId " +
            "AND v.createdAt BETWEEN :startDate AND :endDate",
            AnalyticsViewEntity.class)
            .setParameter("listingId", listingId)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
    }

    private List<AnalyticsClickEntity> getClicksByListing(UUID listingId, LocalDateTime startDate, LocalDateTime endDate) {
        return entityManager.createQuery(
            "SELECT c FROM AnalyticsClickEntity c WHERE c.listing.id = :listingId " +
            "AND c.createdAt BETWEEN :startDate AND :endDate",
            AnalyticsClickEntity.class)
            .setParameter("listingId", listingId)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
    }

    private List<AnalyticsFavoriteEntity> getFavoritesByListing(UUID listingId, LocalDateTime startDate, LocalDateTime endDate) {
        return entityManager.createQuery(
            "SELECT f FROM AnalyticsFavoriteEntity f WHERE f.listing.id = :listingId " +
            "AND f.createdAt BETWEEN :startDate AND :endDate",
            AnalyticsFavoriteEntity.class)
            .setParameter("listingId", listingId)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
    }

    private List<AnalyticsContactEntity> getContactsByListing(UUID listingId, LocalDateTime startDate, LocalDateTime endDate) {
        return entityManager.createQuery(
            "SELECT c FROM AnalyticsContactEntity c WHERE c.listing.id = :listingId " +
            "AND c.createdAt BETWEEN :startDate AND :endDate",
            AnalyticsContactEntity.class)
            .setParameter("listingId", listingId)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
    }

    private List<AnalyticsConversionEntity> getConversionsByListing(UUID listingId, LocalDateTime startDate, LocalDateTime endDate) {
        return entityManager.createQuery(
            "SELECT c FROM AnalyticsConversionEntity c WHERE c.listing.id = :listingId " +
            "AND c.createdAt BETWEEN :startDate AND :endDate",
            AnalyticsConversionEntity.class)
            .setParameter("listingId", listingId)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
    }

    private List<UUID> getOwnerListings(UUID ownerId) {
        return entityManager.createQuery(
            "SELECT l.id FROM ListingEntity l WHERE l.owner.id = :ownerId",
            UUID.class)
            .setParameter("ownerId", ownerId)
            .getResultList();
    }

    private List<AnalyticsViewEntity> getViewsByListings(List<UUID> listingIds, LocalDateTime startDate, LocalDateTime endDate) {
        return entityManager.createQuery(
            "SELECT v FROM AnalyticsViewEntity v WHERE v.listing.id IN :listingIds " +
            "AND v.createdAt BETWEEN :startDate AND :endDate",
            AnalyticsViewEntity.class)
            .setParameter("listingIds", listingIds)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
    }

    private List<AnalyticsClickEntity> getClicksByListings(List<UUID> listingIds, LocalDateTime startDate, LocalDateTime endDate) {
        return entityManager.createQuery(
            "SELECT c FROM AnalyticsClickEntity c WHERE c.listing.id IN :listingIds " +
            "AND c.createdAt BETWEEN :startDate AND :endDate",
            AnalyticsClickEntity.class)
            .setParameter("listingIds", listingIds)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
    }

    private List<AnalyticsFavoriteEntity> getFavoritesByListings(List<UUID> listingIds, LocalDateTime startDate, LocalDateTime endDate) {
        return entityManager.createQuery(
            "SELECT f FROM AnalyticsFavoriteEntity f WHERE f.listing.id IN :listingIds " +
            "AND f.createdAt BETWEEN :startDate AND :endDate",
            AnalyticsFavoriteEntity.class)
            .setParameter("listingIds", listingIds)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
    }

    private List<AnalyticsContactEntity> getContactsByListings(List<UUID> listingIds, LocalDateTime startDate, LocalDateTime endDate) {
        return entityManager.createQuery(
            "SELECT c FROM AnalyticsContactEntity c WHERE c.listing.id IN :listingIds " +
            "AND c.createdAt BETWEEN :startDate AND :endDate",
            AnalyticsContactEntity.class)
            .setParameter("listingIds", listingIds)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
    }

    private List<AnalyticsConversionEntity> getConversionsByListings(List<UUID> listingIds, LocalDateTime startDate, LocalDateTime endDate) {
        return entityManager.createQuery(
            "SELECT c FROM AnalyticsConversionEntity c WHERE c.listing.id IN :listingIds " +
            "AND c.createdAt BETWEEN :startDate AND :endDate",
            AnalyticsConversionEntity.class)
            .setParameter("listingIds", listingIds)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
    }

    private AnalyticsReportDto createEmptyReport(UUID ownerId, String period) {
        AnalyticsReportDto report = new AnalyticsReportDto();
        report.id = UUID.randomUUID();
        report.ownerId = ownerId;
        report.period = period;
        report.reportType = "owner";
        report.generatedAt = LocalDateTime.now();
        
        // Créer des métriques vides
        AnalyticsMetricsDto metrics = new AnalyticsMetricsDto();
        metrics.totalViews = 0L;
        metrics.uniqueViews = 0L;
        metrics.totalClicks = 0L;
        metrics.totalFavorites = 0L;
        metrics.totalContacts = 0L;
        metrics.totalConversions = 0L;
        metrics.conversionRate = 0.0;
        metrics.clickThroughRate = 0.0;
        metrics.favoriteRate = 0.0;
        metrics.contactRate = 0.0;
        
        report.metrics = metrics;
        report.breakdown = new AnalyticsBreakdownDto();
        report.timeSeries = new AnalyticsTimeSeriesDto();
        report.recommendations = new ArrayList<>();
        report.summary = new AnalyticsSummaryDto();
        report.charts = new HashMap<>();
        
        return report;
    }
}
