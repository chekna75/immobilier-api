package com.ditsolution.features.storage.resource;

import com.ditsolution.features.storage.service.S3CleanupService;
import com.ditsolution.features.storage.service.ThumbnailService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

@Path("/admin/storage")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Storage Admin", description = "Administration du stockage et des miniatures")
@RolesAllowed("ADMIN")
public class StorageAdminResource {

    private static final Logger LOG = Logger.getLogger(StorageAdminResource.class);

    @Inject
    ThumbnailService thumbnailService;

    @Inject
    S3CleanupService s3CleanupService;

    @POST
    @Path("/thumbnails/generate")
    @Operation(
        summary = "Générer toutes les miniatures manquantes",
        description = "Force la génération de miniatures pour toutes les images qui n'en ont pas encore"
    )
    @APIResponse(
        responseCode = "200",
        description = "Génération de miniatures lancée avec succès"
    )
    @APIResponse(
        responseCode = "403",
        description = "Accès refusé - Admin requis"
    )
    public Response generateAllThumbnails() {
        try {
            LOG.info("Génération manuelle de toutes les miniatures lancée par un admin");
            int generatedCount = thumbnailService.generateThumbnailsForAllImages();
            
            return Response.ok(new ThumbnailGenerationResponse(
                true, 
                "Génération terminée", 
                generatedCount
            )).build();
            
        } catch (Exception e) {
            LOG.error("Erreur lors de la génération manuelle des miniatures", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Erreur lors de la génération des miniatures: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/cleanup/unused")
    @Operation(
        summary = "Nettoyer les images non utilisées",
        description = "Supprime physiquement les images non utilisées depuis plus de 30 jours"
    )
    @APIResponse(
        responseCode = "200",
        description = "Nettoyage lancé avec succès"
    )
    @APIResponse(
        responseCode = "403",
        description = "Accès refusé - Admin requis"
    )
    public Response cleanupUnusedImages() {
        try {
            LOG.info("Nettoyage manuel des images non utilisées lancé par un admin");
            S3CleanupService.CleanupResult result = s3CleanupService.cleanupUnusedImages();
            
            return Response.ok(new CleanupResponse(
                true,
                "Nettoyage terminé",
                result
            )).build();
            
        } catch (Exception e) {
            LOG.error("Erreur lors du nettoyage manuel des images", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Erreur lors du nettoyage: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/cleanup/all-unused")
    @Operation(
        summary = "Nettoyer toutes les images non utilisées",
        description = "Supprime physiquement toutes les images non utilisées (sans limite de temps)"
    )
    @APIResponse(
        responseCode = "200",
        description = "Nettoyage complet lancé avec succès"
    )
    @APIResponse(
        responseCode = "403",
        description = "Accès refusé - Admin requis"
    )
    public Response cleanupAllUnusedImages() {
        try {
            LOG.info("Nettoyage complet manuel des images non utilisées lancé par un admin");
            S3CleanupService.CleanupResult result = s3CleanupService.cleanupAllUnusedImages();
            
            return Response.ok(new CleanupResponse(
                true,
                "Nettoyage complet terminé",
                result
            )).build();
            
        } catch (Exception e) {
            LOG.error("Erreur lors du nettoyage complet manuel des images", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Erreur lors du nettoyage complet: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/stats")
    @Operation(
        summary = "Statistiques du stockage",
        description = "Retourne les statistiques sur les images et miniatures"
    )
    @APIResponse(
        responseCode = "200",
        description = "Statistiques récupérées avec succès"
    )
    @APIResponse(
        responseCode = "403",
        description = "Accès refusé - Admin requis"
    )
    public Response getStorageStats() {
        try {
            // Compter les images sans miniatures
            var imagesWithoutThumbnails = com.ditsolution.features.storage.entity.UploadedImageEntity.findImagesWithoutThumbnails();
            var unusedImages = com.ditsolution.features.storage.entity.UploadedImageEntity.findUnusedImages();
            var oldUnusedImages = com.ditsolution.features.storage.entity.UploadedImageEntity.findOldUnusedImages(30);
            
            StorageStats stats = new StorageStats();
            stats.imagesWithoutThumbnails = imagesWithoutThumbnails.size();
            stats.unusedImages = unusedImages.size();
            stats.oldUnusedImages = oldUnusedImages.size();
            
            return Response.ok(stats).build();
            
        } catch (Exception e) {
            LOG.error("Erreur lors de la récupération des statistiques", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Erreur lors de la récupération des statistiques: " + e.getMessage()))
                    .build();
        }
    }

    // Classes de réponse
    public static class ThumbnailGenerationResponse {
        public boolean success;
        public String message;
        public int generatedCount;

        public ThumbnailGenerationResponse(boolean success, String message, int generatedCount) {
            this.success = success;
            this.message = message;
            this.generatedCount = generatedCount;
        }
    }

    public static class CleanupResponse {
        public boolean success;
        public String message;
        public S3CleanupService.CleanupResult result;

        public CleanupResponse(boolean success, String message, S3CleanupService.CleanupResult result) {
            this.success = success;
            this.message = message;
            this.result = result;
        }
    }

    public static class StorageStats {
        public int imagesWithoutThumbnails;
        public int unusedImages;
        public int oldUnusedImages;
    }

    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
