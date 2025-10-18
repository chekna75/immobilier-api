package com.ditsolution.features.storage.service;

import com.ditsolution.features.storage.entity.UploadedImageEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class LocalS3Service {

    @ConfigProperty(name = "aws.s3.region", defaultValue = "eu-north-1")
    String region;

    @ConfigProperty(name = "aws.s3.bucket", defaultValue = "app-immo")
    String bucket;

    @ConfigProperty(name = "app.upload.presigned-url-expiration-seconds", defaultValue = "3600")
    int presignedUrlExpirationSeconds;

    public String generatePresignedUrl(String fileName, String contentType, String userId) {
        // Simuler une URL pré-signée pour les tests
        String uniqueFileName = generateUniqueFileName(fileName);
        String key = "users/" + userId + "/listings/" + uniqueFileName;
        
        // URL simulée (pour les tests)
        return String.format("https://%s.s3.%s.amazonaws.com/%s?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=test&X-Amz-Date=20250101T000000Z&X-Amz-Expires=%d&X-Amz-SignedHeaders=host&X-Amz-Signature=test", 
                           bucket, region, key, presignedUrlExpirationSeconds);
    }

    public String getPublicUrl(String fileName, String userId) {
        // URL publique simulée
        return String.format("https://%s.s3.%s.amazonaws.com/users/%s/listings/%s", bucket, region, userId, fileName);
    }

    public String getPublicUrlFromKey(String key) {
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
}

