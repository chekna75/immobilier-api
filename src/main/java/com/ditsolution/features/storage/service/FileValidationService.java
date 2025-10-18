package com.ditsolution.features.storage.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class FileValidationService {

    @ConfigProperty(name = "app.upload.max-file-size-mb", defaultValue = "10")
    int maxFileSizeMb;

    @ConfigProperty(name = "app.upload.allowed-formats", defaultValue = "jpg,jpeg,png,webp")
    String allowedFormats;

    @ConfigProperty(name = "app.upload.max-photos-per-listing", defaultValue = "5")
    int maxPhotosPerListing;

    @ConfigProperty(name = "app.upload.min-photos-per-listing", defaultValue = "1")
    int minPhotosPerListing;

    public boolean isValidFileFormat(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }

        String extension = getFileExtension(fileName).toLowerCase();
        List<String> allowedFormatsList = Arrays.asList(allowedFormats.split(","));
        
        return allowedFormatsList.contains(extension);
    }

    public boolean isValidFileSize(long fileSizeBytes) {
        long maxSizeBytes = maxFileSizeMb * 1024 * 1024; // Convertir MB en bytes
        return fileSizeBytes <= maxSizeBytes;
    }

    public boolean isValidPhotoCount(int photoCount) {
        return photoCount >= minPhotosPerListing && photoCount <= maxPhotosPerListing;
    }

    public boolean hasMinimumPhotos(int photoCount) {
        return photoCount >= minPhotosPerListing;
    }

    public String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    public int getMaxFileSizeMb() {
        return maxFileSizeMb;
    }

    public List<String> getAllowedFormats() {
        return Arrays.asList(allowedFormats.split(","));
    }

    public int getMaxPhotosPerListing() {
        return maxPhotosPerListing;
    }

    public int getMinPhotosPerListing() {
        return minPhotosPerListing;
    }
}
