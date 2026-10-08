package com.healthdev.fisiovital.treatment.interfaces.rest;

import jakarta.validation.constraints.NotNull;

public record BookSessionRequest(
        @NotNull Long patientId,
        @NotNull Long slotId
) {
}
