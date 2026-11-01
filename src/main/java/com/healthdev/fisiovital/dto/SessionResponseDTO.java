package com.healthdev.fisiovital.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** lateChange = true cuando el cambio o la cancelacion se hizo con menos de 24 horas (US11, US12). */
@Schema(example = "{\"id\":5,\"planId\":1,\"sessionNumber\":3,\"totalSessions\":12,\"status\":\"RESERVED\","
        + "\"slotId\":12,\"startTime\":\"2026-10-19T10:00:00\",\"endTime\":\"2026-10-19T11:00:00\","
        + "\"patientName\":\"Carlos Mendoza\",\"physiotherapistName\":\"Mariana Rojas\",\"lateChange\":false}")
public record SessionResponseDTO(Long id, Long planId, int sessionNumber, int totalSessions, String status,
                                 Long slotId, LocalDateTime startTime, LocalDateTime endTime,
                                 String patientName, String physiotherapistName, boolean lateChange) {
}
