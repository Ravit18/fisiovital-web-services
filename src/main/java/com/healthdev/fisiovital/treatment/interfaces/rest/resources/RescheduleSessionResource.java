package com.healthdev.fisiovital.treatment.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;

public record RescheduleSessionResource(@NotNull(message = "{validation.required}") Long newSlotId) {
}
