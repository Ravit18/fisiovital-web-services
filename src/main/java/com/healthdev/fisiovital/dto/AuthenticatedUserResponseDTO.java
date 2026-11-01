package com.healthdev.fisiovital.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = "{\"userId\":1,\"profileId\":1,\"email\":\"carlos.mendoza@gmail.com\","
        + "\"role\":\"PATIENT\",\"fullName\":\"Carlos Mendoza\",\"token\":\"eyJhbGciOiJIUzI1NiJ9...\"}")
public record AuthenticatedUserResponseDTO(Long userId, Long profileId, String email, String role,
                                           String fullName, String token) {
}
