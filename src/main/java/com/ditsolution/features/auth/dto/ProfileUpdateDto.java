package com.ditsolution.features.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateDto(
    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    String email,
    
    @NotBlank(message = "Le numéro de téléphone est requis")
    @Size(min = 8, max = 15, message = "Le numéro de téléphone doit contenir entre 8 et 15 caractères")
    String phone,
    
    String currentPassword, // Optionnel - requis seulement si on change le mot de passe
    
    @Size(min = 6, message = "Le nouveau mot de passe doit contenir au moins 6 caractères")
    String newPassword
) {}
