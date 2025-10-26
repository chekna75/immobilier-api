package com.ditsolution.features.social.resource;

import com.ditsolution.features.social.dto.*;
import com.ditsolution.features.social.service.ReviewService;
import com.ditsolution.features.social.service.SocialShareService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.UUID;

@Path("/api/social-analytics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Social Analytics", description = "Statistiques et analytics des fonctionnalités sociales")
public class SocialAnalyticsResource {

    private static final Logger LOG = Logger.getLogger(SocialAnalyticsResource.class);

    @Inject
    ReviewService reviewService;

    @Inject
    SocialShareService socialShareService;

    @Inject
    SecurityIdentity securityIdentity;

    /**
     * Obtenir les statistiques complètes d'une cible (avis + partages)
     */
    @GET
    @Path("/target/{targetId}")
    @Operation(summary = "Statistiques complètes d'une cible", description = "Obtenir les statistiques d'avis et de partage pour une cible")
    public Response getTargetAnalytics(
            @PathParam("targetId") UUID targetId,
            @QueryParam("targetType") @DefaultValue("property") String targetType) {
        try {
            // Statistiques d'avis
            ReviewStatsDto reviewStats = reviewService.getReviewStats(targetId, targetType);
            
            // Statistiques de partage (si c'est une propriété)
            SocialShareStatsDto shareStats = null;
            if ("property".equals(targetType)) {
                shareStats = socialShareService.getShareStats(targetId);
            }

            // Créer la réponse combinée
            var analytics = new java.util.HashMap<String, Object>();
            analytics.put("targetId", targetId);
            analytics.put("targetType", targetType);
            analytics.put("reviewStats", reviewStats);
            analytics.put("shareStats", shareStats);

            return Response.ok(analytics).build();
        } catch (Exception e) {
            LOG.error("Erreur lors du calcul des analytics de cible", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Obtenir les statistiques globales des fonctionnalités sociales
     */
    @GET
    @Path("/global")
    @Operation(summary = "Statistiques globales", description = "Obtenir les statistiques globales des fonctionnalités sociales")
    @RolesAllowed("ADMIN")
    public Response getGlobalAnalytics() {
        try {
            // Statistiques globales de partage
            SocialShareStatsDto shareStats = socialShareService.getGlobalShareStats();
            
            // Statistiques globales d'avis (à implémenter)
            var reviewStats = new java.util.HashMap<String, Object>();
            reviewStats.put("totalReviews", 0L);
            reviewStats.put("pendingReviews", 0L);
            reviewStats.put("reportedReviews", 0L);

            // Créer la réponse combinée
            var analytics = new java.util.HashMap<String, Object>();
            analytics.put("shareStats", shareStats);
            analytics.put("reviewStats", reviewStats);
            analytics.put("generatedAt", java.time.OffsetDateTime.now());

            return Response.ok(analytics).build();
        } catch (Exception e) {
            LOG.error("Erreur lors du calcul des analytics globales", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Obtenir les métriques de performance sociale
     */
    @GET
    @Path("/performance")
    @Operation(summary = "Métriques de performance", description = "Obtenir les métriques de performance des fonctionnalités sociales")
    @RolesAllowed("ADMIN")
    public Response getPerformanceMetrics() {
        try {
            // TODO: Implémenter les métriques de performance
            var metrics = new java.util.HashMap<String, Object>();
            metrics.put("message", "Métriques de performance à implémenter");
            metrics.put("generatedAt", java.time.OffsetDateTime.now());

            return Response.ok(metrics).build();
        } catch (Exception e) {
            LOG.error("Erreur lors du calcul des métriques de performance", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Obtenir les tendances sociales
     */
    @GET
    @Path("/trends")
    @Operation(summary = "Tendances sociales", description = "Obtenir les tendances des fonctionnalités sociales")
    @RolesAllowed("ADMIN")
    public Response getSocialTrends(@QueryParam("period") @DefaultValue("7d") String period) {
        try {
            // TODO: Implémenter l'analyse des tendances
            var trends = new java.util.HashMap<String, Object>();
            trends.put("message", "Analyse des tendances à implémenter");
            trends.put("period", period);
            trends.put("generatedAt", java.time.OffsetDateTime.now());

            return Response.ok(trends).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de l'analyse des tendances", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
}
