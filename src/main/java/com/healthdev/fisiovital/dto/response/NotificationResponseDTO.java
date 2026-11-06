package com.healthdev.fisiovital.dto.response;

import java.time.LocalDateTime;

public record NotificationResponseDTO(
        Long id,
        String type,
        String message,
        boolean read,
        LocalDateTime createdAt
) {}
