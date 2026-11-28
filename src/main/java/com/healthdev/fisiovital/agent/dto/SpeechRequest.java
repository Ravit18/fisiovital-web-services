package com.healthdev.fisiovital.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Texto de una respuesta del asistente que se quiere escuchar. */
public record SpeechRequest(
        @NotBlank(message = "{validation.required}")
        @Size(max = 1000, message = "{validation.assistant.message.size}")
        String text
) {}
