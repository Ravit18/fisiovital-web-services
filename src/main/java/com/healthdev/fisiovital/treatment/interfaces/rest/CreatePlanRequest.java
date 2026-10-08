package com.healthdev.fisiovital.treatment.interfaces.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePlanRequest(
        @NotNull Long patientId,
        @NotNull Long physiotherapistId,
        @NotBlank @Size(max = 300) String diagnosis,
        @Min(value = 1, message = "Indica un número de sesiones entre 1 y 30")
        @Max(value = 30, message = "Indica un número de sesiones entre 1 y 30") int totalSessions,
        @Min(1) @Max(30) int sessionsPerWeek
) {
}
