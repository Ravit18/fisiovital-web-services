package com.healthdev.fisiovital.dto.response;

import java.time.LocalDate;
import java.util.List;

/** Horarios libres de un fisioterapeuta. nextAvailableWeek se llena cuando la semana pedida esta llena (US06). */
public record AvailabilityResponseDTO(
        Long physiotherapistId,
        LocalDate from,
        LocalDate to,
        List<SlotResponseDTO> slots,
        LocalDate nextAvailableWeek,
        String message
) {}
