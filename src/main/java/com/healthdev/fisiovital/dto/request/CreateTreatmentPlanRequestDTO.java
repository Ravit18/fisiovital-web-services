package com.healthdev.fisiovital.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTreatmentPlanRequestDTO(
        @NotNull(message = "{validation.required}")
        Long physiotherapistId,

        @NotBlank(message = "{validation.required}")
        @Size(max = 255)
        String diagnosis,

        @NotNull(message = "{validation.sessions.range}")
        @Min(value = 1, message = "{validation.sessions.range}")
        @Max(value = 30, message = "{validation.sessions.range}")
        Integer totalSessions,

        @NotNull(message = "{validation.required}")
        @Min(1)
        @Max(7)
        Integer frequencyPerWeek
) {}
