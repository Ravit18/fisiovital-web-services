package com.healthdev.fisiovital.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(example = "{\"newSlotId\":11}")
public record RescheduleSessionRequestDTO(@NotNull(message = "{validation.required}") Long newSlotId) {
}
