package com.healthdev.fisiovital.dto.response;

import java.time.LocalDateTime;

/** lateChange = true cuando el cambio o la cancelacion se hizo con menos de 24 horas (US11, US12). */
public record SessionResponseDTO(
        Long id,
        Long planId,
        int sessionNumber,
        int totalSessions,
        String status,
        Long slotId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String patientName,
        String physiotherapistName,
        boolean lateChange
) {}
