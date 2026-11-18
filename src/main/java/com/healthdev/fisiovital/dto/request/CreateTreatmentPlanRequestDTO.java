package com.healthdev.fisiovital.dto.request;


@Schema(example = "{\"physiotherapistId\":1,\"diagnosis\":\"Lumbalgia cronica\","
        + "\"totalSessions\":12,\"frequencyPerWeek\":2}")
public record CreateTreatmentPlanRequestDTO(
        @NotNull(message = "{validation.sessions.range}")
        @Min(value = 1, message = "{validation.sessions.range}")
