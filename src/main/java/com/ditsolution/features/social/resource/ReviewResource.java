package com.ditsolution.features.social.resource;

import com.ditsolution.features.social.dto.*;
import com.ditsolution.features.social.service.ReviewService;
import com.ditsolution.features.auth.entity.UserEntity;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@Path("/api/reviews")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Reviews", description = "Gestion des avis et évaluations")
public class ReviewResource {

    private static final Logger LOG = Logger.getLogger(ReviewResource.class);

    @Inject
    ReviewService reviewService;

    @Inject
    SecurityIdentity securityIdentity;

    /**
     * Créer un nouvel avis
     */
    @POST
    @Operation(summary = "Créer un avis", description = "Créer un nouvel avis pour un bien, propriétaire ou agence")
    @RolesAllowed({"TENANT", "OWNER", "ADMIN"})
    public Response createReview(@Valid CreateReviewRequest request) {
        try {
            UserEntity currentUser = getCurrentUser();
            ReviewDto review = reviewService.createReview(request, currentUser);
            return Response.ok(review).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la création de l'avis", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Récupérer les avis d'une cible
     */
    @GET
    @Operation(summary = "Récupérer les avis", description = "Récupérer tous les avis d'une cible (bien, propriétaire, agence)")
    public Response getReviews(
            @QueryParam("targetId") UUID targetId,
            @QueryParam("targetType") @DefaultValue("property") String targetType,
            @QueryParam("status") String status) {
        try {
            UUID currentUserId = getCurrentUser() != null ? getCurrentUser().getId() : null;
            List<ReviewDto> reviews = reviewService.getReviews(targetId, targetType, status, currentUserId);
            return Response.ok(reviews).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des avis", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Marquer un avis comme utile
     */
    @POST
    @Path("/{reviewId}/helpful")
    @Operation(summary = "Marquer comme utile", description = "Marquer ou retirer le marquage d'un avis comme utile")
    @RolesAllowed({"TENANT", "OWNER", "ADMIN"})
    public Response markAsHelpful(@PathParam("reviewId") UUID reviewId) {
        try {
            UserEntity currentUser = getCurrentUser();
            boolean isHelpful = reviewService.markAsHelpful(reviewId, currentUser.getId());
            return Response.ok("{\"isHelpful\": " + isHelpful + "}").build();
        } catch (Exception e) {
            LOG.error("Erreur lors du marquage de l'avis", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Signaler un avis
     */
    @POST
    @Path("/{reviewId}/report")
    @Operation(summary = "Signaler un avis", description = "Signaler un avis inapproprié")
    @RolesAllowed({"TENANT", "OWNER", "ADMIN"})
    public Response reportReview(@PathParam("reviewId") UUID reviewId) {
        try {
            UserEntity currentUser = getCurrentUser();
            boolean reported = reviewService.reportReview(reviewId, currentUser.getId());
            return Response.ok("{\"reported\": " + reported + "}").build();
        } catch (Exception e) {
            LOG.error("Erreur lors du signalement de l'avis", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Obtenir les statistiques d'avis
     */
    @GET
    @Path("/stats")
    @Operation(summary = "Statistiques des avis", description = "Obtenir les statistiques des avis pour une cible")
    public Response getReviewStats(
            @QueryParam("targetId") UUID targetId,
            @QueryParam("targetType") @DefaultValue("property") String targetType) {
        try {
            ReviewStatsDto stats = reviewService.getReviewStats(targetId, targetType);
            return Response.ok(stats).build();
        } catch (Exception e) {
            LOG.error("Erreur lors du calcul des statistiques", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Récupérer les avis d'un utilisateur
     */
    @GET
    @Path("/user/{userId}")
    @Operation(summary = "Avis d'un utilisateur", description = "Récupérer tous les avis laissés par un utilisateur")
    public Response getUserReviews(@PathParam("userId") UUID userId) {
        try {
            // TODO: Implémenter la récupération des avis d'un utilisateur spécifique
            return Response.ok("{\"message\": \"Fonctionnalité à implémenter\"}").build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des avis utilisateur", e);
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
