package com.healthdev.fisiovital.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(example = "{\"date\":\"2026-10-19\",\"items\":[{\"slotId\":10,\"startTime\":\"2026-10-19T08:00:00\","
        + "\"endTime\":\"2026-10-19T09:00:00\",\"status\":\"BOOKED\",\"sessionId\":5,"
        + "\"patientName\":\"Carlos Mendoza\",\"sessionNumber\":3,\"totalSessions\":12,\"sessionStatus\":\"RESERVED\"}],"
        + "\"message\":null}")
public record AgendaResponseDTO(LocalDate date, List<AgendaItemResponseDTO> items, String message) {
}
