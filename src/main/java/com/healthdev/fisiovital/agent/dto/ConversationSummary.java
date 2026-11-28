package com.healthdev.fisiovital.agent.dto;

import java.time.LocalDateTime;

/** Una conversacion del historial de chats del usuario, de la mas reciente a la mas antigua. */
public record ConversationSummary(
        Long id,
        String title,
        LocalDateTime startedAt,
        LocalDateTime lastActivityAt
) {}
