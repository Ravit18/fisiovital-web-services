package com.healthdev.fisiovital.mapper;

import com.healthdev.fisiovital.dto.response.AssistantReplyResponseDTO;
import com.healthdev.fisiovital.model.Conversation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface AssistantMapper {

    /** source: AI (respuesta de Gemini) o SAFETY (respuesta fija de seguridad). */
    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "reply", source = "reply")
    @Mapping(target = "source", source = "source")
    AssistantReplyResponseDTO toResponse(Conversation conversation, String reply, String source);
}
