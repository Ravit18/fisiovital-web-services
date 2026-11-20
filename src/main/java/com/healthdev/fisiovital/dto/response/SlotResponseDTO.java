package com.healthdev.fisiovital.dto.response;

import java.time.LocalDateTime;

public record SlotResponseDTO(
        Long id,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status
) {}
