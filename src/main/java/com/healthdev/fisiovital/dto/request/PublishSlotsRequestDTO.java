package com.healthdev.fisiovital.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(example = "{\"date\":\"2026-10-19\",\"startTime\":\"08:00\",\"endTime\":\"12:00\",\"durationMinutes\":60}")
public record PublishSlotsRequestDTO(
        @NotNull(message = "{validation.required}") LocalDate date,
        @NotNull(message = "{validation.required}") @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @NotNull(message = "{validation.required}") @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        @NotNull(message = "{validation.required}") @Min(15) @Max(240) Integer durationMinutes) {
}
