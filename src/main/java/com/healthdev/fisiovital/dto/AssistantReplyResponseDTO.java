package com.healthdev.fisiovital.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** source: AI (respuesta de Gemini) o SAFETY (respuesta fija de seguridad, sin consultar al modelo). */
@Schema(example = "{\"conversationId\":3,\"reply\":\"Puedes cancelar sin penalidad hasta 24 horas antes de tu "
        + "sesión.\",\"source\":\"AI\"}")
public record AssistantReplyResponseDTO(Long conversationId, String reply, String source) {
}
