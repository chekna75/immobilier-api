package com.ditsolution.features.storage.resource;

import com.ditsolution.features.auth.entity.UserEntity;
import com.ditsolution.features.storage.dto.UploadRequestDto;
import com.ditsolution.features.storage.dto.UploadResponseDto;
import com.ditsolution.features.storage.service.FileValidationService;
import com.ditsolution.features.storage.service.S3Service;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;
import com.ditsolution.features.storage.entity.UploadedImageEntity;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/storage")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Storage", description = "Gestion du stockage de fichiers")
public class StorageResource {

    @Inject
    S3Service s3Service;

    @Inject
    FileValidationService fileValidationService;

    @Inject
    JsonWebToken jwt;

    @ConfigProperty(name = "aws.s3.region")
    String region;

    @ConfigProperty(name = "aws.s3.bucket")
    String bucket;

    @ConfigProperty(name = "aws.s3.access-key")
    String accessKey;

    @ConfigProperty(name = "aws.s3.secret-key")
    String secretKey;

    private software.amazon.awssdk.services.s3.S3Client s3Client;

    private void initializeClients() {
        if (s3Client == null) {
            software.amazon.awssdk.auth.credentials.AwsBasicCredentials awsCredentials = 
                software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(accessKey, secretKey);
            software.amazon.awssdk.auth.credentials.StaticCredentialsProvider credentialsProvider = 
                software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(awsCredentials);

            s3Client = software.amazon.awssdk.services.s3.S3Client.builder()
                    .region(software.amazon.awssdk.regions.Region.of(region))
                    .credentialsProvider(credentialsProvider)
                    .build();
        }
    }

    @POST
    @Path("/sign")
    @RolesAllowed({"OWNER", "TENANT"})
    @Operation(
        summary = "Générer une URL pré-signée pour l'upload",
        description = "Génère une URL pré-signée S3 pour permettre l'upload direct d'un fichier. L'image sera associée à l'utilisateur connecté."
    )
    @APIResponse(
        responseCode = "200",
        description = "URL pré-signée générée avec succès",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = UploadResponseDto.class)
        )
    )
    @APIResponse(
        responseCode = "400",
        description = "Format de fichier non autorisé ou paramètres invalides"
    )
    @APIResponse(
        responseCode = "401",
        description = "Non authentifié"
    )
    @APIResponse(
        responseCode = "403",
        description = "Non autorisé"
    )
    public Response generatePresignedUrl(UploadRequestDto request) {
        try {
            // Récupérer l'utilisateur connecté
            UserEntity currentUser = getCurrentUser();
            if (currentUser == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse("Utilisateur non trouvé"))
                        .build();
            }

            // Validation du format de fichier
            if (!fileValidationService.isValidFileFormat(request.getFileName())) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Format de fichier non autorisé. Formats acceptés: " + 
                                String.join(", ", fileValidationService.getAllowedFormats())))
                        .build();
            }

            // Validation du content type
            if (request.getContentType() == null || request.getContentType().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Content-Type est requis"))
                        .build();
            }

            // Générer l'URL pré-signée avec l'ID utilisateur
            String uploadUrl = s3Service.generatePresignedUrl(request.getFileName(), request.getContentType(), currentUser.getId().toString());
            
            // Extraire le nom de fichier généré de l'URL
            String fileName = extractFileNameFromUrl(uploadUrl);
            String publicUrl = s3Service.getPublicUrl(fileName, currentUser.getId().toString());
            String s3Key = s3Service.extractS3KeyFromUrl(uploadUrl);

            // Enregistrer les informations de l'image dans la base de données
            s3Service.recordUploadedImage(
                currentUser.getId(),
                request.getFileName(),
                fileName,
                request.getContentType(),
                null, // fileSize sera mis à jour après l'upload
                s3Key,
                publicUrl
            );

            UploadResponseDto response = new UploadResponseDto();
            response.setUploadUrl(uploadUrl);
            response.setPublicUrl(publicUrl);
            response.setFileName(fileName);
            response.setUserId(currentUser.getId().toString());

            return Response.ok(response).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Erreur lors de la génération de l'URL pré-signée: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/my-images")
    @RolesAllowed({"OWNER", "TENANT"})
    @Operation(
        summary = "Récupérer les images uploadées par l'utilisateur connecté",
        description = "Retourne la liste des images uploadées par l'utilisateur connecté"
    )
    @APIResponse(
        responseCode = "200",
        description = "Liste des images récupérée avec succès"
    )
    @APIResponse(
        responseCode = "401",
        description = "Non authentifié"
    )
    public Response getMyImages() {
        try {
            UserEntity currentUser = getCurrentUser();
            if (currentUser == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse("Utilisateur non trouvé"))
                        .build();
            }

            var images = UploadedImageEntity.findByUserId(currentUser.getId());
            return Response.ok(images).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Erreur lors de la récupération des images: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/my-images/unused")
    @RolesAllowed({"OWNER", "TENANT"})
    @Operation(
        summary = "Récupérer les images non utilisées par l'utilisateur connecté",
        description = "Retourne la liste des images uploadées mais non encore utilisées dans une annonce"
    )
    @APIResponse(
        responseCode = "200",
        description = "Liste des images non utilisées récupérée avec succès"
    )
    @APIResponse(
        responseCode = "401",
        description = "Non authentifié"
    )
    public Response getMyUnusedImages() {
        try {
            UserEntity currentUser = getCurrentUser();
            if (currentUser == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse("Utilisateur non trouvé"))
                        .build();
            }

            var images = UploadedImageEntity.findUnusedByUserId(currentUser.getId());
            return Response.ok(images).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Erreur lors de la récupération des images: " + e.getMessage()))
                    .build();
        }
    }

    private UserEntity getCurrentUser() {
        if (jwt == null || jwt.getSubject() == null) {
            return null;
        }
        
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            return UserEntity.findById(userId);
        } catch (Exception e) {
            return null;
        }
    }

    @GET
    @Path("/image/{userId}/{fileName}")
    @Operation(
        summary = "Servir une image via l'API",
        description = "Endpoint proxy pour servir les images S3 via l'API"
    )
    public Response serveImage(@PathParam("userId") String userId, @PathParam("fileName") String fileName) {
        try {
            // Construire l'URL S3
            String s3Url = String.format("https://app-immo.s3.eu-north-1.amazonaws.com/users/%s/listings/%s", 
                userId, fileName);
            
            // Faire une requête vers S3 pour récupérer l'image
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(s3Url))
                .GET()
                .build();
            
            java.net.http.HttpResponse<byte[]> response = client.send(request, 
                java.net.http.HttpResponse.BodyHandlers.ofByteArray());
            
            if (response.statusCode() == 200) {
                // Déterminer le type de contenu
                String contentType = "image/jpeg"; // par défaut
                if (fileName.toLowerCase().endsWith(".png")) {
                    contentType = "image/png";
                } else if (fileName.toLowerCase().endsWith(".webp")) {
                    contentType = "image/webp";
                }
                
                return Response.ok(response.body())
                    .type(contentType)
                    .header("Cache-Control", "public, max-age=3600")
                    .build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Image non trouvée")
                    .build();
            }
            
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Erreur lors de la récupération de l'image: " + e.getMessage())
                .build();
        }
    }

    @POST
    @Path("/upload")
    @RolesAllowed({"OWNER", "TENANT"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(
        summary = "Upload direct d'image via le backend",
        description = "Upload direct d'une image via le backend pour contourner les problèmes CORS"
    )
    @APIResponse(
        responseCode = "200",
        description = "Image uploadée avec succès",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = UploadResponseDto.class)
        )
    )
    @APIResponse(
        responseCode = "400",
        description = "Format de fichier non autorisé ou paramètres invalides"
    )
    @APIResponse(
        responseCode = "401",
        description = "Non authentifié"
    )
    public Response uploadImageDirect() {
        try {
            UserEntity currentUser = getCurrentUser();
            if (currentUser == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse("Utilisateur non trouvé"))
                        .build();
            }

            // Pour l'instant, retourner une erreur indiquant que cette fonctionnalité n'est pas encore implémentée
            return Response.status(Response.Status.NOT_IMPLEMENTED)
                    .entity(new ErrorResponse("Upload direct non encore implémenté. Veuillez configurer CORS sur S3."))
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Erreur lors de l'upload: " + e.getMessage()))
                    .build();
        }
    }

    private String generateUniqueFileName(String originalFileName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomString = java.util.UUID.randomUUID().toString().substring(0, 8);
        String extension = originalFileName.substring(originalFileName.lastIndexOf('.'));
        return String.format("listing_%s_%s%s", timestamp, randomString, extension);
    }

    private String extractFileNameFromUrl(String uploadUrl) {
        try {
            java.net.URI uri = java.net.URI.create(uploadUrl);
            String path = uri.getPath(); // ex: /users/{id}/listings/uuid.jpg
            if (path == null || path.isEmpty()) return null;
            String last = path.substring(path.lastIndexOf('/') + 1);
            return last; // sans query
        } catch (Exception e) {
            String last = uploadUrl.substring(uploadUrl.lastIndexOf('/') + 1);
            int q = last.indexOf('?');
            return q >= 0 ? last.substring(0, q) : last;
        }
    }

    // Classe interne pour les réponses d'erreur
    public static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
