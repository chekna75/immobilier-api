package com.ditsolution.features.social.resource;

import com.ditsolution.features.social.dto.*;
import com.ditsolution.features.social.service.SocialShareService;
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

@Path("/api/social-shares")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Social Shares", description = "Gestion des partages sociaux")
public class SocialShareResource {

    private static final Logger LOG = Logger.getLogger(SocialShareResource.class);

    @Inject
    SocialShareService socialShareService;

    @Inject
    SecurityIdentity securityIdentity;

    /**
     * Enregistrer un partage social
     */
    @POST
    @Operation(summary = "Enregistrer un partage", description = "Enregistrer un partage sur les réseaux sociaux")
    @RolesAllowed({"TENANT", "OWNER", "ADMIN"})
    public Response recordShare(@Valid CreateSocialShareRequest request) {
        try {
            UserEntity currentUser = getCurrentUser();
            SocialShareDto share = socialShareService.recordShare(request, currentUser);
            return Response.ok(share).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de l'enregistrement du partage", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Récupérer les partages d'une annonce
     */
    @GET
    @Path("/listing/{listingId}")
    @Operation(summary = "Partages d'une annonce", description = "Récupérer tous les partages d'une annonce")
    public Response getListingShares(@PathParam("listingId") UUID listingId) {
        try {
            List<SocialShareDto> shares = socialShareService.getListingShares(listingId);
            return Response.ok(shares).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des partages", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Récupérer les partages d'un utilisateur
     */
    @GET
    @Path("/user/{userId}")
    @Operation(summary = "Partages d'un utilisateur", description = "Récupérer tous les partages d'un utilisateur")
    @RolesAllowed({"TENANT", "OWNER", "ADMIN"})
    public Response getUserShares(@PathParam("userId") UUID userId) {
        try {
            List<SocialShareDto> shares = socialShareService.getUserShares(userId);
            return Response.ok(shares).build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des partages utilisateur", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Obtenir les statistiques de partage d'une annonce
     */
    @GET
    @Path("/stats/listing/{listingId}")
    @Operation(summary = "Statistiques de partage d'une annonce", description = "Obtenir les statistiques de partage pour une annonce")
    public Response getListingShareStats(@PathParam("listingId") UUID listingId) {
        try {
            SocialShareStatsDto stats = socialShareService.getShareStats(listingId);
            return Response.ok(stats).build();
        } catch (Exception e) {
            LOG.error("Erreur lors du calcul des statistiques de partage", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Obtenir les statistiques globales de partage
     */
    @GET
    @Path("/stats/global")
    @Operation(summary = "Statistiques globales de partage", description = "Obtenir les statistiques globales de partage")
    @RolesAllowed("ADMIN")
    public Response getGlobalShareStats() {
        try {
            SocialShareStatsDto stats = socialShareService.getGlobalShareStats();
            return Response.ok(stats).build();
        } catch (Exception e) {
            LOG.error("Erreur lors du calcul des statistiques globales", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Récupérer les partages récents
     */
    @GET
    @Path("/recent")
    @Operation(summary = "Partages récents", description = "Récupérer les partages récents")
    @RolesAllowed("ADMIN")
    public Response getRecentShares(@QueryParam("limit") @DefaultValue("50") int limit) {
        try {
            // TODO: Implémenter la récupération des partages récents
            return Response.ok("{\"message\": \"Fonctionnalité à implémenter\"}").build();
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des partages récents", e);
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
