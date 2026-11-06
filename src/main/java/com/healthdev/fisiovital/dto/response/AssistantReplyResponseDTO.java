package com.healthdev.fisiovital.dto.response;

/** source: AI (respuesta de Gemini) o SAFETY (respuesta fija de seguridad, sin consultar al modelo). */
public record AssistantReplyResponseDTO(
        Long conversationId,
        String reply,
        String source
) {}
