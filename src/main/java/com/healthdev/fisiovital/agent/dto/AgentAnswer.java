package com.healthdev.fisiovital.agent.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Salida estructurada del modelo: la respuesta del asistente convertida en un objeto Java. choices son las
 * respuestas rapidas que el chat muestra como botones (horarios, fisioterapeutas, confirmar); al tocar una, se
 * envia su texto como el siguiente mensaje del usuario. action es la pantalla que se abre (asistente por voz).
 */
public record AgentAnswer(
        @JsonPropertyDescription("Respuesta para el usuario, en texto plano, sin formato JSON ni markdown de codigo")
        String reply,
        @JsonPropertyDescription("Opciones cortas que el usuario puede tocar como respuesta (maximo 6, cada una de "
                + "maximo 40 caracteres). Vacio si no hay que elegir nada")
        List<String> choices,
        @JsonPropertyDescription("Pantalla que se abre en la Web App para el usuario; null si no hay que cambiar")
        AgentAction action) {

    public AgentAnswer(String reply) {
        this(reply, List.of(), null);
    }

    public AgentAnswer(String reply, List<String> choices) {
        this(reply, choices, null);
    }

    /** Opciones limpias: sin vacias ni repetidas, cortas y como maximo {@value #MAX_CHOICES}. */
    public List<String> cleanChoices() {
        if (choices == null) {
            return List.of();
        }
        return choices.stream()
                .filter(c -> c != null && !c.isBlank())
                .map(String::strip)
                .map(c -> c.length() > MAX_CHOICE_LENGTH ? c.substring(0, MAX_CHOICE_LENGTH).strip() : c)
                .distinct()
                .limit(MAX_CHOICES)
                .toList();
    }

    public static final int MAX_CHOICES = 6;
    public static final int MAX_CHOICE_LENGTH = 60;
}
