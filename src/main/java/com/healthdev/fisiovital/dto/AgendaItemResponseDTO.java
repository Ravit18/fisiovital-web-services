package com.healthdev.fisiovital.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** status: FREE, BOOKED o FREE_CANCELLED (bloque liberado porque el paciente cancelo). */
@Schema(example = "{\"slotId\":10,\"startTime\":\"2026-10-19T08:00:00\",\"endTime\":\"2026-10-19T09:00:00\","
        + "\"status\":\"BOOKED\",\"sessionId\":5,\"patientName\":\"Carlos Mendoza\",\"sessionNumber\":3,"
        + "\"totalSessions\":12,\"sessionStatus\":\"RESERVED\"}")
public record AgendaItemResponseDTO(Long slotId, LocalDateTime startTime, LocalDateTime endTime, String status,
                                    Long sessionId, String patientName, Integer sessionNumber,
                                    Integer totalSessions, String sessionStatus) {
}
