package com.ditsolution.features.storage.dto;

import lombok.Data;

@Data
public class UploadResponseDto {
    private String uploadUrl;    // URL pré-signée pour l'upload
    private String publicUrl;    // URL publique pour accéder au fichier
    private String fileName;     // Nom du fichier généré
    private String userId;       // ID de l'utilisateur
}
