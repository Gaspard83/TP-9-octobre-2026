package com.boutique.security.dto;

public record AuthResponse(String token, String type, long expiresIn) {

    public static AuthResponse bearer(String token, long expiresIn) {
        return new AuthResponse(token, "Bearer", expiresIn);
    }
}
