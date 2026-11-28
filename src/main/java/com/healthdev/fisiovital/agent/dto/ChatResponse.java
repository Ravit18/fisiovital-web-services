package com.healthdev.fisiovital.agent.dto;

import java.util.List;

/**
 * source: AI (respuesta de Gemini) o SAFETY (respuesta fija de seguridad, sin consultar al modelo).
 * choices: respuestas rapidas para mostrar como botones (puede venir vacia).
 * action: pantalla que la Web App debe abrir (asistente por voz); null si no hay que navegar.
 */
public record ChatResponse(
        Long conversationId,
        String reply,
        String source,
        List<String> choices,
        AgentAction action
) {}
