package com.healthdev.fisiovital.treatment.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;

public record BookSessionResource(@NotNull(message = "{validation.required}") Long slotId) {
}
