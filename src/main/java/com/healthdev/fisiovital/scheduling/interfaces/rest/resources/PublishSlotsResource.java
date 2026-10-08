package com.healthdev.fisiovital.scheduling.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/** Ejemplo: { "date": "2026-10-19", "startTime": "08:00", "endTime": "12:00", "durationMinutes": 60 } */
public record PublishSlotsResource(
        @NotNull(message = "{validation.required}") LocalDate date,
        @NotNull(message = "{validation.required}") @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @NotNull(message = "{validation.required}") @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        @NotNull(message = "{validation.required}") @Min(15) @Max(240) Integer durationMinutes) {
}
