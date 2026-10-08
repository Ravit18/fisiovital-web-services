package com.healthdev.fisiovital.treatment.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IdentityRequest(
        @NotBlank @Size(max = 150) String name
) {
}
