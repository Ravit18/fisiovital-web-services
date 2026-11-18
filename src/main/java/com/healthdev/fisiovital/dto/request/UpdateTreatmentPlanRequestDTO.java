package com.healthdev.fisiovital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(example = "{\"diagnosis\":\"Lumbalgia cronica\",\"totalSessions\":10,\"frequencyPerWeek\":3}")
public record UpdateTreatmentPlanRequestDTO(
        @NotBlank(message = "{validation.required}") @Size(max = 255) String diagnosis,
        @NotNull(message = "{validation.sessions.range}")
        @Min(value = 1, message = "{validation.sessions.range}")
        @Max(value = 30, message = "{validation.sessions.range}") Integer totalSessions,
        @NotNull(message = "{validation.required}") @Min(1) @Max(7) Integer frequencyPerWeek) {
}
