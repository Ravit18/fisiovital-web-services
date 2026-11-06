package com.healthdev.fisiovital.dto.response;

import java.time.LocalDateTime;

public record TreatmentPlanResponseDTO(
        Long id,
        Long patientId,
        String patientName,
        Long physiotherapistId,
        String physiotherapistName,
        String diagnosis,
        int totalSessions,
        int frequencyPerWeek,
        String status,
        LocalDateTime createdAt,
        long bookedSessions,
        long completedSessions
) {}
