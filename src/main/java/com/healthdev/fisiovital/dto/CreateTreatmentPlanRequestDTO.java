package com.healthdev.fisiovital.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(example = "{\"physiotherapistId\":1,\"diagnosis\":\"Lumbalgia cronica\","
        + "\"totalSessions\":12,\"frequencyPerWeek\":2}")
public record CreateTreatmentPlanRequestDTO(
        @NotNull(message = "{validation.required}") Long physiotherapistId,
        @NotBlank(message = "{validation.required}") @Size(max = 255) String diagnosis,
        @NotNull(message = "{validation.sessions.range}")
        @Min(value = 1, message = "{validation.sessions.range}")
        @Max(value = 30, message = "{validation.sessions.range}") Integer totalSessions,
        @NotNull(message = "{validation.required}") @Min(1) @Max(7) Integer frequencyPerWeek) {
}
