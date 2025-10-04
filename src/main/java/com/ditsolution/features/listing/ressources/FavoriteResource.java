package com.ditsolution.features.listing.ressources;

import com.ditsolution.features.listing.dto.FavoriteDto;
import com.ditsolution.features.listing.service.FavoriteService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/favorites")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FavoriteResource {

    @Inject
    FavoriteService favoriteService;

    @Inject
    JsonWebToken jwt;

    /**
     * Récupère tous les favoris de l'utilisateur connecté
     */
    @GET
    @RolesAllowed({"TENANT", "OWNER", "ADMIN"})
    public Response getUserFavorites() {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            List<FavoriteDto> favorites = favoriteService.getUserFavorites(userId);
            return Response.ok(favorites).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erreur lors de la récupération des favoris")
                    .build();
        }
    }

    /**
     * Ajoute un bien aux favoris
     */
    @POST
    @Path("/{listingId}")
    @RolesAllowed({"TENANT", "OWNER", "ADMIN"})
    public Response addFavorite(@PathParam("listingId") UUID listingId) {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            FavoriteDto favorite = favoriteService.addFavorite(userId, listingId);
            return Response.ok(favorite).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Bien non trouvé")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erreur lors de l'ajout aux favoris")
                    .build();
        }
    }

    /**
     * Supprime un bien des favoris
     */
    @DELETE
    @Path("/{listingId}")
    @RolesAllowed({"TENANT", "OWNER", "ADMIN"})
    public Response removeFavorite(@PathParam("listingId") UUID listingId) {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            favoriteService.removeFavorite(userId, listingId);
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erreur lors de la suppression des favoris")
                    .build();
        }
    }

    /**
     * Bascule l'état favori d'un bien
     */
    @POST
    @Path("/{listingId}/toggle")
    @RolesAllowed({"TENANT", "OWNER", "ADMIN"})
    public Response toggleFavorite(@PathParam("listingId") UUID listingId) {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            boolean isFavorite = favoriteService.toggleFavorite(userId, listingId);
            return Response.ok().entity("{\"isFavorite\": " + isFavorite + "}").build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Bien non trouvé")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erreur lors de la mise à jour des favoris")
                    .build();
        }
    }

    /**
     * Vérifie si un bien est dans les favoris
     */
    @GET
    @Path("/{listingId}/check")
    @RolesAllowed({"TENANT", "OWNER", "ADMIN"})
    public Response checkFavorite(@PathParam("listingId") UUID listingId) {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            boolean isFavorite = favoriteService.isFavorite(userId, listingId);
            return Response.ok().entity("{\"isFavorite\": " + isFavorite + "}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erreur lors de la vérification des favoris")
                    .build();
        }
    }
}
