package com.healthdev.fisiovital.treatment.interfaces.rest.resources;

import java.time.LocalDateTime;

/** status: FREE, BOOKED o FREE_CANCELLED (bloque liberado porque el paciente cancelo). */
public record AgendaItemResource(Long slotId, LocalDateTime startTime, LocalDateTime endTime, String status,
                                 Long sessionId, String patientName, Integer sessionNumber,
                                 Integer totalSessions, String sessionStatus) {
}
