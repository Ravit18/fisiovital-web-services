package com.healthdev.fisiovital.dto.response;

import java.time.LocalDateTime;

/** status: FREE, BOOKED o FREE_CANCELLED (bloque liberado porque el paciente cancelo). */
public record AgendaItemResponseDTO(
        Long slotId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        Long sessionId,
        String patientName,
        Integer sessionNumber,
        Integer totalSessions,
        String sessionStatus
) {}
