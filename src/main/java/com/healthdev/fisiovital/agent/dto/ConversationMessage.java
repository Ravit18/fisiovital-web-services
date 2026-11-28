package com.healthdev.fisiovital.agent.dto;

import java.time.LocalDateTime;
import java.util.List;

/** sender: USER o ASSISTANT. choices: opciones que el asistente ofrecio en ese mensaje (puede venir vacia). */
public record ConversationMessage(
        Long id,
        String sender,
        String content,
        List<String> choices,
        LocalDateTime createdAt
) {}
