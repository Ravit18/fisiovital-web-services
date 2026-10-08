package com.healthdev.fisiovital.treatment.interfaces.rest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompleteSessionRequest(
        @NotNull Long physiotherapistId,
        @Size(max = 2000) String observation
) {
}
