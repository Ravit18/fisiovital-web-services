package com.healthdev.fisiovital.agent.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Salida estructurada del modelo: la respuesta del asistente convertida en un objeto Java. */
public record AgentAnswer(
        @JsonPropertyDescription("Respuesta para el usuario, en texto plano, sin formato JSON ni markdown de codigo")
        String reply) {
}
