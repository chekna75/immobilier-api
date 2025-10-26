package com.ditsolution.features.social.resource;

import com.ditsolution.features.social.dto.ReviewDto;
import com.ditsolution.features.social.service.ReviewIntegrationService;
import com.ditsolution.features.auth.entity.UserEntity;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/reviews-integration")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Reviews Integration", description = "Intégration avec le système d'avis du frontend")
public class ReviewIntegrationResource {

    private static final Logger LOG = Logger.getLogger(ReviewIntegrationResource.class);

    @Inject
    ReviewIntegrationService integrationService;

    @Inject
    SecurityIdentity securityIdentity;

    /**
     * Créer un avis depuis le frontend (compatible avec ReviewsService.js)
     */
    @POST
    @Operation(summary = "Créer un avis", description = "Endpoint compatible avec ReviewsService.createReview du frontend")
    @RolesAllowed({"TENANT", "OWNER", "ADMIN"})
    public Response createReview(Map<String, Object> reviewData) {
        try {
            UserEntity currentUser = getCurrentUser();
            if (currentUser == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Utilisateur non authentifié\"}")
                    .build();
            }

            ReviewDto review = integrationService.createReviewFromFrontend(reviewData, currentUser);
            return Response.ok(review).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la création d'avis depuis le frontend", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Récupérer les avis depuis le frontend (compatible avec ReviewsService.js)
     */
    @GET
    @Operation(summary = "Récupérer les avis", description = "Endpoint compatible avec ReviewsService.getReviews du frontend")
    public Response getReviews(
            @QueryParam("targetId") UUID targetId,
            @QueryParam("category") @DefaultValue("property") String category,
            @QueryParam("status") @DefaultValue("approved") String status) {
        try {
            Map<String, Object> filters = Map.of("status", status);
            List<ReviewDto> reviews = integrationService.getReviewsForFrontend(targetId, category, filters);
            return Response.ok(reviews).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des avis", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Obtenir les statistiques d'avis pour le frontend
     */
    @GET
    @Path("/stats")
    @Operation(summary = "Statistiques d'avis", description = "Obtenir les statistiques d'avis pour le frontend")
    public Response getReviewStats(
            @QueryParam("targetId") UUID targetId,
            @QueryParam("category") @DefaultValue("property") String category) {
        try {
            Map<String, Object> stats = integrationService.getReviewStatsForFrontend(targetId, category);
            return Response.ok(stats).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des stats", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Marquer un avis comme utile depuis le frontend
     */
    @POST
    @Path("/{reviewId}/helpful")
    @Operation(summary = "Marquer comme utile", description = "Marquer un avis comme utile depuis le frontend")
    @RolesAllowed({"TENANT", "OWNER", "ADMIN"})
    public Response markAsHelpful(@PathParam("reviewId") Long reviewId) {
        try {
            UserEntity currentUser = getCurrentUser();
            if (currentUser == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Utilisateur non authentifié\"}")
                    .build();
            }

            boolean isHelpful = integrationService.markAsHelpfulFromFrontend(reviewId, currentUser.getId());
            return Response.ok("{\"isHelpful\": " + isHelpful + "}").build();
        } catch (Exception e) {
            LOG.error("Erreur lors du marquage utile", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Signaler un avis depuis le frontend
     */
    @POST
    @Path("/{reviewId}/report")
    @Operation(summary = "Signaler un avis", description = "Signaler un avis depuis le frontend")
    @RolesAllowed({"TENANT", "OWNER", "ADMIN"})
    public Response reportReview(@PathParam("reviewId") Long reviewId) {
        try {
            UserEntity currentUser = getCurrentUser();
            if (currentUser == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Utilisateur non authentifié\"}")
                    .build();
            }

            boolean reported = integrationService.reportReviewFromFrontend(reviewId, currentUser.getId());
            return Response.ok("{\"reported\": " + reported + "}").build();
        } catch (Exception e) {
            LOG.error("Erreur lors du signalement", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Obtenir l'utilisateur actuel
     */
    private UserEntity getCurrentUser() {
        var principal = securityIdentity == null ? null : securityIdentity.getPrincipal();
        if (principal == null) return null;
        try { 
            return UserEntity.findById(UUID.fromString(principal.getName())); 
        } catch (Exception e) { 
            LOG.error("Erreur lors de la récupération de l'utilisateur", e);
            return null; 
        }
    }
}
