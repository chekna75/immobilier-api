package com.ditsolution.features.social.dto;

import jakarta.validation.constraints.*;
import java.util.Map;
import java.util.UUID;

public class CreateReviewRequest {
    @NotNull(message = "L'ID de la cible est requis")
    public UUID targetId;

    @NotBlank(message = "Le type de cible est requis")
    @Pattern(regexp = "^(property|owner|agency)$", message = "Type de cible invalide")
    public String targetType;

    @NotNull(message = "La note globale est requise")
    @DecimalMin(value = "1.0", message = "La note doit être au minimum 1")
    @DecimalMax(value = "5.0", message = "La note doit être au maximum 5")
    public java.math.BigDecimal overallRating;

    @Size(max = 255, message = "Le titre ne peut pas dépasser 255 caractères")
    public String title;

    @Size(max = 2000, message = "Le commentaire ne peut pas dépasser 2000 caractères")
    public String comment;

    @NotNull(message = "Les notes détaillées sont requises")
    public Map<String, java.math.BigDecimal> ratings;

    public CreateReviewRequest() {}

    public CreateReviewRequest(UUID targetId, String targetType, java.math.BigDecimal overallRating, 
                              String title, String comment, Map<String, java.math.BigDecimal> ratings) {
        this.targetId = targetId;
        this.targetType = targetType;
        this.overallRating = overallRating;
        this.title = title;
        this.comment = comment;
        this.ratings = ratings;
    }
}
