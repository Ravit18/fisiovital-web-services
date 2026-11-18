package com.healthdev.fisiovital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Historial de citas. US14 responde paginado (page/size/totalElements/totalPages);
 * US15 y US16 responden sin paginacion (los campos de pagina quedan en null).
 */

    /** Respuesta sin paginacion (US15 y US16). */
    public AppointmentHistoryResponseDTO(List<AppointmentResponseDTO> appointments, String message) {
        this(appointments, message, null, null, null, null);
    }
}
