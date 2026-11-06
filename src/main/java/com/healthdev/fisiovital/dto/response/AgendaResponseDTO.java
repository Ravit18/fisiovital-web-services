package com.healthdev.fisiovital.dto.response;

import java.time.LocalDate;
import java.util.List;

public record AgendaResponseDTO(
        LocalDate date,
        List<AgendaItemResponseDTO> items,
        String message
) {}
