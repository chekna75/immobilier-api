package com.ditsolution.features.auth.dto;

import java.time.OffsetDateTime;

public class AuthDtos {
    public record RegisterRequest(String email, String password, String phone, String firstName, String lastName, String role) {}
    public record LoginEmailRequest(String email, String password) {}
    public record RequestOtpRequest(String phone) {}
    public record LoginOtpRequest(String phone, String code) {}
    public record RefreshRequest(String refreshToken) {}
    public record UpdateMeRequest(String firstName, String lastName, String avatarUrl) {}
    public record ChangePasswordRequest(String currentPassword, String newPassword) {}
    public record AuthResponse(String accessToken, String refreshToken, UserDto user) {}
    
    // DTOs pour les demandes de changement de rôle
    public record RoleChangeRequestDto(String requestedRole, String reason) {}
    public record RoleChangeRequestResponseDto(Long id, String requestedRole, String status, String reason, OffsetDateTime createdAt) {}
    
    // DTOs pour la réinitialisation de mot de passe
    public record ForgotPasswordRequest(String email) {}
    public record ResetPasswordRequest(String token, String newPassword) {}
}


