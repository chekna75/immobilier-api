package com.ditsolution.features.auth.dto;

public class AuthDtos {
    public record RegisterRequest(String email, String password, String phone, String firstName, String lastName, String role) {}
    public record LoginEmailRequest(String email, String password) {}
    public record RequestOtpRequest(String phone) {}
    public record LoginOtpRequest(String phone, String code) {}
    public record RefreshRequest(String refreshToken) {}
    public record UpdateMeRequest(String firstName, String lastName, String avatarUrl) {}
    public record AuthResponse(String accessToken, String refreshToken, UserDto user) {}
}


