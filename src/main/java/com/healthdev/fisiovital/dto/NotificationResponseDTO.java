package com.healthdev.fisiovital.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(example = "{\"id\":7,\"type\":\"SESSION_RESCHEDULED\","
        + "\"message\":\"Carlos Mendoza reprogramó su sesión del 19/10/2026 a las 10:00.\","
        + "\"read\":false,\"createdAt\":\"2026-10-18T09:30:00\"}")
public record NotificationResponseDTO(Long id, String type, String message, boolean read, LocalDateTime createdAt) {
}
