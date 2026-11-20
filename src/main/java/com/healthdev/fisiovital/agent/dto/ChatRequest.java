package com.healthdev.fisiovital.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** conversationId es opcional: sin el (o si la conversacion vencio) se inicia una nueva. */
public record ChatRequest(
        Long conversationId,
        @NotBlank(message = "{validation.required}")
        @Size(max = 1000, message = "{validation.assistant.message.size}")
        String message
) {}
