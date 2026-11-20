package com.healthdev.fisiovital.dto.response;

public record AuthenticatedUserResponseDTO(
        Long userId,
        Long profileId,
        String email,
        String role,
        String fullName,
        String token
) {}
