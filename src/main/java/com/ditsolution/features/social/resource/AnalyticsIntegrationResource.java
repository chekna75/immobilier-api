package com.ditsolution.features.social.resource;

import com.ditsolution.features.social.service.SocialShareIntegrationService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.UUID;

@Path("/api/analytics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Analytics Integration", description = "Intégration avec le système d'analytics du frontend")
public class AnalyticsIntegrationResource {

    private static final Logger LOG = Logger.getLogger(AnalyticsIntegrationResource.class);

    @Inject
    SocialShareIntegrationService integrationService;

    @Inject
    SecurityIdentity securityIdentity;

    /**
     * Endpoint pour tracker les conversions depuis le frontend
     * Compatible avec AnalyticsService.trackConversion
     */
    @POST
    @Path("/track-conversion")
    @Operation(summary = "Tracker une conversion", description = "Endpoint compatible avec AnalyticsService.trackConversion du frontend")
    @RolesAllowed({"TENANT", "OWNER", "ADMIN"})
    public Response trackConversion(
            @QueryParam("listingId") String listingId,
            @QueryParam("userId") String userId,
            @QueryParam("conversionType") String conversionType,
            Map<String, Object> metadata) {
        try {
            // Tracker les partages sociaux
            if ("social_share".equals(conversionType) && metadata != null) {
                String platform = metadata.get("platform") != null ? 
                    metadata.get("platform").toString() : "unknown";
                String shareType = metadata.get("shareType") != null ? 
                    metadata.get("shareType").toString() : "property";
                
                integrationService.trackShareFromFrontend(
                    listingId, userId, platform, shareType, metadata
                );
            }

            return Response.ok("{\"success\": true}").build();
        } catch (Exception e) {
            LOG.error("Erreur lors du tracking de conversion", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Obtenir les statistiques de partage pour le frontend
     */
    @GET
    @Path("/share-stats/{listingId}")
    @Operation(summary = "Statistiques de partage", description = "Obtenir les statistiques de partage pour une annonce")
    public Response getShareStats(@PathParam("listingId") UUID listingId) {
        try {
            Map<String, Object> stats = integrationService.getShareStatsForFrontend(listingId);
            return Response.ok(stats).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des stats", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }
}
