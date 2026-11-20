package com.healthdev.fisiovital.dto.request;

import jakarta.validation.constraints.NotNull;

public record RescheduleSessionRequestDTO(
        @NotNull(message = "{validation.required}")
        Long newSlotId
) {}
