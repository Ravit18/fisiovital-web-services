package com.healthdev.fisiovital.agent.dto;

/** source: AI (respuesta de Gemini) o SAFETY (respuesta fija de seguridad, sin consultar al modelo). */
public record ChatResponse(
        Long conversationId,
        String reply,
        String source
) {}
