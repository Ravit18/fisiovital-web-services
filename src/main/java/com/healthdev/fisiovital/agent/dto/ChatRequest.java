package com.healthdev.fisiovital.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * conversationId es opcional: sin el se inicia un chat nuevo; con el se retoma ese chat del historial.
 * screen es la ruta que el usuario tiene abierta (por ejemplo /patient/physiotherapists/5) y voice indica que la
 * respuesta se va a leer en voz alta (asistente flotante): asi el asistente sabe "de quien" se habla y responde corto.
 */
public record ChatRequest(
        Long conversationId,
        @NotBlank(message = "{validation.required}")
        @Size(max = 1000, message = "{validation.assistant.message.size}")
        String message,
        @Size(max = 200)
        String screen,
        Boolean voice
) {

    public ChatRequest(Long conversationId, String message) {
        this(conversationId, message, null, null);
    }
}
