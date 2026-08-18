package com.vetclinic.api.auth.dto;

public record LoginResponse(
        String token,
        String tokenType,
        UserProfile user
) {
    public record UserProfile(String id, String name, String email, String role) {
    }
}
