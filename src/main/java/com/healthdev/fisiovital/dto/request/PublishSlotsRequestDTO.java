package com.healthdev.fisiovital.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record PublishSlotsRequestDTO(
        @NotNull(message = "{validation.required}")
        LocalDate date,

        @NotNull(message = "{validation.required}")
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,

        @NotNull(message = "{validation.required}")
        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,

        // Cada sesión dura 1 hora, igual que en la Web App (SESSION_MINUTES = 60).
        @NotNull(message = "{validation.required}")
        @Min(value = 60, message = "{validation.slots.duration}")
        @Max(value = 60, message = "{validation.slots.duration}")
        Integer durationMinutes
) {}
