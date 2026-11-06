package com.healthdev.fisiovital.dto.response;

import java.time.LocalDateTime;

public record AppointmentResponseDTO(
        Long sessionId,
        int sessionNumber,
        int totalSessions,
        String status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long patientId,
        String patientName,
        Long physiotherapistId,
        String physiotherapistName,
        String specialty,
        String diagnosis
) {}
