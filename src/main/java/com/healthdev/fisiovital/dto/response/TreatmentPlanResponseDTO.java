package com.healthdev.fisiovital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(example = "{\"id\":1,\"patientId\":1,\"patientName\":\"Carlos Mendoza\",\"physiotherapistId\":1,"
        + "\"physiotherapistName\":\"Mariana Rojas\",\"diagnosis\":\"Lumbalgia cronica\",\"totalSessions\":12,"
        + "\"frequencyPerWeek\":2,\"status\":\"ACTIVE\",\"createdAt\":\"2026-10-12T10:15:00\","
        + "\"bookedSessions\":3,\"completedSessions\":0}")
public record TreatmentPlanResponseDTO(Long id, Long patientId, String patientName, Long physiotherapistId,
                                       String physiotherapistName, String diagnosis, int totalSessions,
                                       int frequencyPerWeek, String status, LocalDateTime createdAt,
                                       long bookedSessions, long completedSessions) {
}
