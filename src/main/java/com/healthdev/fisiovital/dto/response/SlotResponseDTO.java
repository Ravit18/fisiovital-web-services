package com.healthdev.fisiovital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(example = "{\"id\":10,\"startTime\":\"2026-10-19T08:00:00\",\"endTime\":\"2026-10-19T09:00:00\","
        + "\"status\":\"FREE\"}")
public record SlotResponseDTO(Long id, LocalDateTime startTime, LocalDateTime endTime, String status) {
}
