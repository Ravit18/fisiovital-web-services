package com.healthdev.fisiovital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(example = "{\"email\":\"carlos.mendoza@gmail.com\",\"password\":\"Fisio2026*\"}")
public record SignInRequestDTO(
        @NotBlank(message = "{validation.required}") String email,
        @NotBlank(message = "{validation.required}") String password) {
}
