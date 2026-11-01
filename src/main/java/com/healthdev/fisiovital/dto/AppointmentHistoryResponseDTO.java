package com.healthdev.fisiovital.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Historial de citas. US14 responde paginado (page/size/totalElements/totalPages);
 * US15 y US16 responden sin paginacion (los campos de pagina quedan en null).
 */
@Schema(example = "{\"appointments\":[{\"sessionId\":5,\"sessionNumber\":3,\"totalSessions\":12,"
        + "\"status\":\"RESERVED\",\"startTime\":\"2026-10-19T10:00:00\",\"endTime\":\"2026-10-19T11:00:00\","
        + "\"patientId\":1,\"patientName\":\"Carlos Mendoza\",\"physiotherapistId\":1,"
        + "\"physiotherapistName\":\"Mariana Rojas\",\"specialty\":\"DEPORTIVA\","
        + "\"diagnosis\":\"Lumbalgia cronica\"}],\"message\":null,\"page\":0,\"size\":5,"
        + "\"totalElements\":3,\"totalPages\":1}")
public record AppointmentHistoryResponseDTO(List<AppointmentResponseDTO> appointments, String message,
                                            Integer page, Integer size, Long totalElements, Integer totalPages) {

    /** Respuesta sin paginacion (US15 y US16). */
    public AppointmentHistoryResponseDTO(List<AppointmentResponseDTO> appointments, String message) {
        this(appointments, message, null, null, null, null);
    }
}
