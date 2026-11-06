package com.healthdev.fisiovital.dto.response;

import java.util.List;

/**
 * Historial de citas. US14 responde paginado (page/size/totalElements/totalPages);
 * US15 y US16 responden sin paginacion (los campos de pagina quedan en null).
 */
public record AppointmentHistoryResponseDTO(
        List<AppointmentResponseDTO> appointments,
        String message,
        Integer page,
        Integer size,
        Long totalElements,
        Integer totalPages
) {

    /** Respuesta sin paginacion (US15 y US16). */
    public AppointmentHistoryResponseDTO(List<AppointmentResponseDTO> appointments, String message) {
        this(appointments, message, null, null, null, null);
    }
}
