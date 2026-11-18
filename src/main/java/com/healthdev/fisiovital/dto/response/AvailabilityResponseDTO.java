package com.healthdev.fisiovital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/** Horarios libres de un fisioterapeuta. nextAvailableWeek se llena cuando la semana pedida esta llena (US06). */
@Schema(example = "{\"physiotherapistId\":1,\"from\":\"2026-10-19\",\"to\":\"2026-10-25\","
        + "\"slots\":[{\"id\":10,\"startTime\":\"2026-10-19T08:00:00\",\"endTime\":\"2026-10-19T09:00:00\","
        + "\"status\":\"FREE\"}],\"nextAvailableWeek\":null,\"message\":null}")
public record AvailabilityResponseDTO(Long physiotherapistId, LocalDate from, LocalDate to,
                                      List<SlotResponseDTO> slots, LocalDate nextAvailableWeek, String message) {
}
