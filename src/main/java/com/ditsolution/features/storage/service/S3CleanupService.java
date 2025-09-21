package com.ditsolution.features.storage.service;

import com.ditsolution.features.storage.entity.UploadedImageEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.util.List;

@ApplicationScoped
public class S3CleanupService {

    private static final Logger LOG = Logger.getLogger(S3CleanupService.class);

    @ConfigProperty(name = "aws.s3.region")
    String region;

    @ConfigProperty(name = "aws.s3.bucket")
    String bucket;

    @ConfigProperty(name = "aws.s3.access-key")
    String accessKey;

    @ConfigProperty(name = "aws.s3.secret-key")
    String secretKey;

    @ConfigProperty(name = "app.cleanup.unused-images-days", defaultValue = "30")
    int unusedImagesDays;

    @Inject
    ThumbnailService thumbnailService;

    private S3Client s3Client;

    @Inject
    public S3CleanupService() {
        // Initialisation différée
    }

    private void initializeS3Client() {
        if (s3Client == null) {
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
            StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(awsCredentials);

            s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(credentialsProvider)
                    .build();
        }
    }

    /**
     * Nettoie les images non utilisées depuis plus de X jours
     */
    @Transactional
    public CleanupResult cleanupUnusedImages() {
        LOG.info("Début du nettoyage des images non utilisées (plus de " + unusedImagesDays + " jours)");
        
        List<UploadedImageEntity> oldUnusedImages = UploadedImageEntity.findOldUnusedImages(unusedImagesDays);
        CleanupResult result = new CleanupResult();
        result.totalImages = oldUnusedImages.size();

        LOG.info("Trouvé " + oldUnusedImages.size() + " images non utilisées à supprimer");

        for (UploadedImageEntity image : oldUnusedImages) {
            try {
                // Supprimer l'image originale de S3
                if (deleteImageFromS3(image.getS3Key())) {
                    result.deletedOriginalImages++;
                }

                // Supprimer la miniature de S3 si elle existe
                if (image.getThumbnailS3Key() != null && !image.getThumbnailS3Key().isEmpty()) {
                    if (thumbnailService.deleteThumbnailFromS3(image.getThumbnailS3Key())) {
                        result.deletedThumbnails++;
                    }
                }

                // Supprimer l'enregistrement de la base de données
                image.delete();
                result.deletedDatabaseRecords++;

                LOG.info("Image supprimée: " + image.getFileName());

            } catch (Exception e) {
                LOG.error("Erreur lors de la suppression de l'image: " + image.getFileName(), e);
                result.errors++;
            }
        }

        LOG.info("Nettoyage terminé - Images supprimées: " + result.deletedOriginalImages + 
                ", Miniatures supprimées: " + result.deletedThumbnails + 
                ", Enregistrements DB supprimés: " + result.deletedDatabaseRecords + 
                ", Erreurs: " + result.errors);

        return result;
    }

    /**
     * Nettoie toutes les images non utilisées (sans limite de temps)
     */
    @Transactional
    public CleanupResult cleanupAllUnusedImages() {
        LOG.info("Début du nettoyage de toutes les images non utilisées");
        
        List<UploadedImageEntity> unusedImages = UploadedImageEntity.findUnusedImages();
        CleanupResult result = new CleanupResult();
        result.totalImages = unusedImages.size();

        LOG.info("Trouvé " + unusedImages.size() + " images non utilisées à supprimer");

        for (UploadedImageEntity image : unusedImages) {
            try {
                // Supprimer l'image originale de S3
                if (deleteImageFromS3(image.getS3Key())) {
                    result.deletedOriginalImages++;
                }

                // Supprimer la miniature de S3 si elle existe
                if (image.getThumbnailS3Key() != null && !image.getThumbnailS3Key().isEmpty()) {
                    if (thumbnailService.deleteThumbnailFromS3(image.getThumbnailS3Key())) {
                        result.deletedThumbnails++;
                    }
                }

                // Supprimer l'enregistrement de la base de données
                image.delete();
                result.deletedDatabaseRecords++;

                LOG.info("Image supprimée: " + image.getFileName());

            } catch (Exception e) {
                LOG.error("Erreur lors de la suppression de l'image: " + image.getFileName(), e);
                result.errors++;
            }
        }

        LOG.info("Nettoyage terminé - Images supprimées: " + result.deletedOriginalImages + 
                ", Miniatures supprimées: " + result.deletedThumbnails + 
                ", Enregistrements DB supprimés: " + result.deletedDatabaseRecords + 
                ", Erreurs: " + result.errors);

        return result;
    }

    /**
     * Supprime une image de S3
     */
    private boolean deleteImageFromS3(String s3Key) {
        try {
            initializeS3Client();
            
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            LOG.info("Image supprimée de S3: " + s3Key);
            return true;

        } catch (NoSuchKeyException e) {
            LOG.warn("Image non trouvée dans S3 (déjà supprimée): " + s3Key);
            return true; // Considéré comme un succès car l'objectif est atteint
        } catch (Exception e) {
            LOG.error("Erreur lors de la suppression de l'image de S3: " + s3Key, e);
            return false;
        }
    }

    /**
     * Vérifie si une image existe dans S3
     */
    public boolean imageExistsInS3(String s3Key) {
        try {
            initializeS3Client();
            
            s3Client.headObject(builder -> builder
                    .bucket(bucket)
                    .key(s3Key)
                    .build());
            
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            LOG.error("Erreur lors de la vérification de l'existence de l'image: " + s3Key, e);
            return false;
        }
    }

    /**
     * Nettoie les orphelins S3 (images dans S3 mais pas en base)
     */
    public CleanupResult cleanupOrphanedS3Objects() {
        LOG.info("Début du nettoyage des objets orphelins dans S3");
        // Cette fonctionnalité nécessiterait de lister tous les objets S3
        // et de vérifier s'ils existent en base de données
        // Pour l'instant, on retourne un résultat vide
        CleanupResult result = new CleanupResult();
        LOG.info("Nettoyage des orphelins S3 non implémenté pour l'instant");
        return result;
    }

    public void close() {
        if (s3Client != null) {
            s3Client.close();
        }
    }

    /**
     * Classe pour stocker les résultats du nettoyage
     */
    public static class CleanupResult {
        public int totalImages = 0;
        public int deletedOriginalImages = 0;
        public int deletedThumbnails = 0;
        public int deletedDatabaseRecords = 0;
        public int errors = 0;

        public boolean hasErrors() {
            return errors > 0;
        }

        public int getTotalDeleted() {
            return deletedOriginalImages + deletedThumbnails;
        }

        @Override
        public String toString() {
            return String.format("CleanupResult{total=%d, deletedImages=%d, deletedThumbnails=%d, deletedDB=%d, errors=%d}",
                    totalImages, deletedOriginalImages, deletedThumbnails, deletedDatabaseRecords, errors);
        }
    }
}
