package com.healthdev.fisiovital.dto.response;

import java.time.LocalDateTime;

/** sessionId, sessionNumber y sessionStartTime son null en las notas generales (sin sesion). */
public record ClinicalNoteResponseDTO(
        Long id,
        Long patientId,
        Long physiotherapistId,
        String physiotherapistName,
        Long sessionId,
        Integer sessionNumber,
        LocalDateTime sessionStartTime,
        String text,
        LocalDateTime createdAt
) {}
