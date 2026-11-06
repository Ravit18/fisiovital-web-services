package com.healthdev.fisiovital.dto.request;

import jakarta.validation.constraints.NotNull;

public record BookSessionRequestDTO(
        @NotNull(message = "{validation.required}")
        Long slotId
) {}
