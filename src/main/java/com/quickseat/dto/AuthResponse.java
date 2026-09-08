package com.quickseat.dto;

public record AuthResponse(Long userId, String name, String email, String role,
        String accessToken, String refreshToken, String tokenType) {
}
