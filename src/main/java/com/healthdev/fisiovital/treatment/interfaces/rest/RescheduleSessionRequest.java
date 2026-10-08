package com.healthdev.fisiovital.treatment.interfaces.rest;

import jakarta.validation.constraints.NotNull;

public record RescheduleSessionRequest(
        @NotNull Long patientId,
        @NotNull Long slotId
) {
}
