package com.healthdev.fisiovital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(example = "{\"sessionId\":5,\"sessionNumber\":3,\"totalSessions\":12,\"status\":\"RESERVED\","
        + "\"startTime\":\"2026-10-19T10:00:00\",\"endTime\":\"2026-10-19T11:00:00\",\"patientId\":1,"
        + "\"patientName\":\"Carlos Mendoza\",\"physiotherapistId\":1,\"physiotherapistName\":\"Mariana Rojas\","
        + "\"specialty\":\"DEPORTIVA\",\"diagnosis\":\"Lumbalgia cronica\"}")
public record AppointmentResponseDTO(Long sessionId, int sessionNumber, int totalSessions, String status,
                                     LocalDateTime startTime, LocalDateTime endTime, Long patientId,
                                     String patientName, Long physiotherapistId, String physiotherapistName,
                                     String specialty, String diagnosis) {
}
