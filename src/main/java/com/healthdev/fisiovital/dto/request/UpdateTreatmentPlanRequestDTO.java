package com.healthdev.fisiovital.dto.request;


@Schema(example = "{\"diagnosis\":\"Lumbalgia cronica\",\"totalSessions\":10,\"frequencyPerWeek\":3}")
public record UpdateTreatmentPlanRequestDTO(
        @NotNull(message = "{validation.sessions.range}")
        @Min(value = 1, message = "{validation.sessions.range}")
