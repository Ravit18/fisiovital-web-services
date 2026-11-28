package com.healthdev.fisiovital.agent.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Pantalla que el asistente abre en la Web App (asistente por voz): el front traduce page a una ruta y la abre.
 * Los campos opcionales afinan la pantalla: el fisioterapeuta del perfil, el dia de sus horarios o la especialidad
 * del buscador.
 */
public record AgentAction(
        @JsonPropertyDescription("Pantalla a abrir, una de las de la seccion # PANTALLA; null si no hay que cambiar")
        String page,
        @JsonPropertyDescription("Id del fisioterapeuta (solo para PHYSIOTHERAPIST_PROFILE)")
        Long physiotherapistId,
        @JsonPropertyDescription("Dia a mostrar en formato AAAA-MM-DD (solo para PHYSIOTHERAPIST_PROFILE)")
        String date,
        @JsonPropertyDescription("Especialidad en mayusculas para filtrar (solo para PHYSIOTHERAPISTS)")
        String specialty) {
}
