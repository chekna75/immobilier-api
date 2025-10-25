package com.ditsolution.features.storage.service;

import com.ditsolution.features.storage.entity.UploadedImageEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class S3Service {

    @ConfigProperty(name = "aws.s3.region")
    String region;

    @ConfigProperty(name = "aws.s3.bucket")
    String bucket;

    @ConfigProperty(name = "aws.s3.access-key")
    String accessKey;

    @ConfigProperty(name = "aws.s3.secret-key")
    String secretKey;

    @ConfigProperty(name = "app.upload.presigned-url-expiration-seconds", defaultValue = "3600")
    int presignedUrlExpirationSeconds;

    @Inject
    ThumbnailService thumbnailService;

    private S3Client s3Client;
    private S3Presigner s3Presigner;

    @Inject
    public S3Service() {
        // Initialisation différée pour éviter les problèmes de configuration
    }

    private void initializeClients() {
        if (s3Client == null) {
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
            StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(awsCredentials);

            s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(credentialsProvider)
                    .build();

            s3Presigner = S3Presigner.builder()
                    .region(Region.of(region))
                    .credentialsProvider(credentialsProvider)
                    .build();
        }
    }

    public String generatePresignedUrl(String fileName, String contentType, String userId) {
        initializeClients();

        // Générer un nom de fichier unique avec l'ID utilisateur
        String uniqueFileName = generateUniqueFileName(fileName);
        String key = "users/" + userId + "/listings/" + uniqueFileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .acl(software.amazon.awssdk.services.s3.model.ObjectCannedACL.PUBLIC_READ)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(presignedUrlExpirationSeconds))
                .putObjectRequest(putObjectRequest)
                .build();

        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }

    public String getPublicUrl(String fileName, String userId) {
        // Construire l'URL publique pour accéder au fichier avec l'ID utilisateur
        return String.format("https://%s.s3.%s.amazonaws.com/users/%s/listings/%s", bucket, region, userId, fileName);
    }

    public String getPublicUrlFromKey(String key) {
        // Construire l'URL publique à partir de la clé S3 complète
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    @Transactional
    public UploadedImageEntity recordUploadedImage(UUID userId, String originalFileName, String generatedFileName, 
                                                   String contentType, Long fileSize, String s3Key, String publicUrl) {
        UploadedImageEntity image = new UploadedImageEntity();
        image.setUserId(userId);
        image.setFileName(generatedFileName);
        image.setOriginalName(originalFileName);
        image.setContentType(contentType);
        image.setFileSize(fileSize);
        image.setS3Key(s3Key);
        image.setPublicUrl(publicUrl);
        image.setIsUsed(false);
        image.setCreatedAt(Instant.now());
        image.setUpdatedAt(Instant.now());
        
        image.persist();
        return image;
    }

    public String extractS3KeyFromUrl(String uploadUrl) {
        try {
            java.net.URI uri = java.net.URI.create(uploadUrl);
            String path = uri.getPath(); // ex: /users/{userId}/listings/filename.jpg
            if (path == null || path.isEmpty()) return null;
            // enlever le slash initial
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e) {
            return null;
        }
    }

    private String generateUniqueFileName(String originalFileName) {
        String extension = "";
        if (originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * Déclenche la génération de miniature pour une image après upload
     */
    public void triggerThumbnailGeneration(UploadedImageEntity imageEntity) {
        try {
            // La génération de miniature sera faite de manière asynchrone
            // pour ne pas bloquer l'upload
            thumbnailService.generateThumbnail(imageEntity);
        } catch (Exception e) {
            // Log l'erreur mais ne pas faire échouer l'upload
            System.err.println("Erreur lors de la génération de miniature pour " + imageEntity.getFileName() + ": " + e.getMessage());
        }
    }

    public void close() {
        if (s3Client != null) {
            s3Client.close();
        }
        if (s3Presigner != null) {
            s3Presigner.close();
        }
    }
}
