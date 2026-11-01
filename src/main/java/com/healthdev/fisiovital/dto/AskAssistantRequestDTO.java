package com.healthdev.fisiovital.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** conversationId es opcional: sin el (o si la conversacion vencio) se inicia una nueva. */
@Schema(example = "{\"conversationId\":null,\"message\":\"¿Cuál es la política de cancelación?\"}")
public record AskAssistantRequestDTO(
        Long conversationId,
        @NotBlank(message = "{validation.required}")
        @Size(max = 1000, message = "{validation.assistant.message.size}") String message) {
}
