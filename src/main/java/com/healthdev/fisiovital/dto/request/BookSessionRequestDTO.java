package com.healthdev.fisiovital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(example = "{\"slotId\":10}")
public record BookSessionRequestDTO(@NotNull(message = "{validation.required}") Long slotId) {
}
