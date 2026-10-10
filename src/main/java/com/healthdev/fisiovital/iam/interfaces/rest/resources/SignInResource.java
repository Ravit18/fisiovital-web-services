package com.healthdev.fisiovital.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record SignInResource(
        @NotBlank(message = "{validation.required}") String email,
        @NotBlank(message = "{validation.required}") String password) {
}
