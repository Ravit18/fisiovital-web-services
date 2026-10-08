package com.healthdev.fisiovital.scheduling.interfaces.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateSlotRequest(
        @NotNull LocalDateTime startsAt,
        @Min(1) @Max(240) int durationMinutes
) {
}
