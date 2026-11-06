package com.healthdev.fisiovital.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SignInRequestDTO(
        @NotBlank(message = "{validation.required}")
        String email,

        @NotBlank(message = "{validation.required}")
        String password
) {}
