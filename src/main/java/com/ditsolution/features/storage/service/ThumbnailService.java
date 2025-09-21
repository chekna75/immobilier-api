package com.ditsolution.features.storage.service;

import com.ditsolution.features.storage.entity.UploadedImageEntity;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.im4java.process.ProcessStarter;
import org.jboss.logging.Logger;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ThumbnailService {

    private static final Logger LOG = Logger.getLogger(ThumbnailService.class);

    @ConfigProperty(name = "aws.s3.region")
    String region;

    @ConfigProperty(name = "aws.s3.bucket")
    String bucket;

    @ConfigProperty(name = "aws.s3.access-key")
    String accessKey;

    @ConfigProperty(name = "aws.s3.secret-key")
    String secretKey;

    @ConfigProperty(name = "app.thumbnail.width", defaultValue = "300")
    int thumbnailWidth;

    @ConfigProperty(name = "app.thumbnail.height", defaultValue = "200")
    int thumbnailHeight;

    @ConfigProperty(name = "app.thumbnail.quality", defaultValue = "85")
    int thumbnailQuality;

    @ConfigProperty(name = "imagemagick.path", defaultValue = "/usr/bin/")
    String imageMagickPath;

    private S3Client s3Client;

    @Inject
    public ThumbnailService() {
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

    @PostConstruct
    public void init() {
        // Configurer le chemin ImageMagick
        ProcessStarter.setGlobalSearchPath(imageMagickPath);
    }

    /**
     * Génère une miniature pour une image donnée
     */
    @Transactional
    public boolean generateThumbnail(UploadedImageEntity imageEntity) {
        try {
            LOG.info("Génération de miniature pour l'image: " + imageEntity.getFileName());

            // Télécharger l'image originale depuis S3
            byte[] originalImageData = downloadImageFromS3(imageEntity.getS3Key());
            if (originalImageData == null) {
                LOG.error("Impossible de télécharger l'image originale: " + imageEntity.getS3Key());
                return false;
            }

            // Générer la miniature
            byte[] thumbnailData = createThumbnail(originalImageData);
            if (thumbnailData == null) {
                LOG.error("Impossible de générer la miniature pour: " + imageEntity.getFileName());
                return false;
            }

            // Uploader la miniature vers S3
            String thumbnailS3Key = generateThumbnailS3Key(imageEntity.getS3Key());
            String thumbnailPublicUrl = uploadThumbnailToS3(thumbnailS3Key, thumbnailData);
            
            if (thumbnailPublicUrl == null) {
                LOG.error("Impossible d'uploader la miniature vers S3");
                return false;
            }

            // Mettre à jour l'entité avec les informations de la miniature
            imageEntity.setThumbnailS3Key(thumbnailS3Key);
            imageEntity.setThumbnailPublicUrl(thumbnailPublicUrl);
            imageEntity.setThumbnailGenerated(true);
            imageEntity.setUpdatedAt(Instant.now());
            imageEntity.persist();

            LOG.info("Miniature générée avec succès pour: " + imageEntity.getFileName());
            return true;

        } catch (Exception e) {
            LOG.error("Erreur lors de la génération de miniature pour " + imageEntity.getFileName(), e);
            return false;
        }
    }

    /**
     * Génère des miniatures pour toutes les images qui n'en ont pas encore
     */
    @Transactional
    public int generateThumbnailsForAllImages() {
        List<UploadedImageEntity> imagesWithoutThumbnails = UploadedImageEntity.findImagesWithoutThumbnails();
        int successCount = 0;

        LOG.info("Génération de miniatures pour " + imagesWithoutThumbnails.size() + " images");

        for (UploadedImageEntity image : imagesWithoutThumbnails) {
            if (generateThumbnail(image)) {
                successCount++;
            }
        }

        LOG.info("Miniatures générées: " + successCount + "/" + imagesWithoutThumbnails.size());
        return successCount;
    }

    /**
     * Télécharge une image depuis S3
     */
    private byte[] downloadImageFromS3(String s3Key) {
        try {
            initializeS3Client();
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build();

            try (InputStream inputStream = s3Client.getObject(getObjectRequest)) {
                return inputStream.readAllBytes();
            }

        } catch (Exception e) {
            LOG.error("Erreur lors du téléchargement de l'image depuis S3: " + s3Key, e);
            return null;
        }
    }

    /**
     * Crée une miniature à partir des données d'image
     */
    private byte[] createThumbnail(byte[] imageData) {
        try {
            ConvertCmd cmd = new ConvertCmd();
            IMOperation op = new IMOperation();
            
            // Lire depuis stdin
            op.addImage("-");
            
            // Redimensionner en gardant les proportions
            op.resize(thumbnailWidth, thumbnailHeight, '^');
            
            // Centrer et couper si nécessaire
            op.gravity("center");
            op.crop(thumbnailWidth, thumbnailHeight, 0, 0);
            
            // Qualité JPEG
            op.quality((double) thumbnailQuality);
            
            // Sortie vers stdout
            op.addImage("jpeg:-");

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            cmd.setOutputConsumer(new org.im4java.process.OutputConsumer() {
                @Override
                public void consumeOutput(InputStream inputStream) throws IOException {
                    outputStream.write(inputStream.readAllBytes());
                }
            });

            cmd.run(op, new ByteArrayInputStream(imageData));
            return outputStream.toByteArray();

        } catch (Exception e) {
            LOG.error("Erreur lors de la création de la miniature", e);
            return null;
        }
    }

    /**
     * Génère une clé S3 pour la miniature
     */
    private String generateThumbnailS3Key(String originalS3Key) {
        // Remplacer le nom de fichier par une version thumbnail
        int lastSlashIndex = originalS3Key.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            return "thumbnails/" + originalS3Key;
        }
        
        String path = originalS3Key.substring(0, lastSlashIndex + 1);
        String fileName = originalS3Key.substring(lastSlashIndex + 1);
        
        // Ajouter "_thumb" avant l'extension
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) {
            return path + "thumbnails/" + fileName + "_thumb";
        }
        
        String nameWithoutExt = fileName.substring(0, dotIndex);
        String extension = fileName.substring(dotIndex);
        
        return path + "thumbnails/" + nameWithoutExt + "_thumb" + extension;
    }

    /**
     * Upload une miniature vers S3
     */
    private String uploadThumbnailToS3(String s3Key, byte[] thumbnailData) {
        try {
            initializeS3Client();
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType("image/jpeg")
                    .contentLength((long) thumbnailData.length)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(thumbnailData));

            // Retourner l'URL publique
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, s3Key);

        } catch (Exception e) {
            LOG.error("Erreur lors de l'upload de la miniature vers S3: " + s3Key, e);
            return null;
        }
    }

    /**
     * Supprime une miniature de S3
     */
    public boolean deleteThumbnailFromS3(String thumbnailS3Key) {
        try {
            initializeS3Client();
            
            s3Client.deleteObject(builder -> builder
                    .bucket(bucket)
                    .key(thumbnailS3Key)
                    .build());

            LOG.info("Miniature supprimée de S3: " + thumbnailS3Key);
            return true;

        } catch (Exception e) {
            LOG.error("Erreur lors de la suppression de la miniature de S3: " + thumbnailS3Key, e);
            return false;
        }
    }

    public void close() {
        if (s3Client != null) {
            s3Client.close();
        }
    }
}
