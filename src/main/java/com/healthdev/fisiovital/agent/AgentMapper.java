package com.healthdev.fisiovital.agent;

import com.healthdev.fisiovital.mapper.MapStructConfig;

import com.healthdev.fisiovital.agent.dto.AgentAction;
import com.healthdev.fisiovital.agent.dto.ChatResponse;
import com.healthdev.fisiovital.model.Conversation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = MapStructConfig.class)
public interface AgentMapper {

    /** source: AI (respuesta de Gemini) o SAFETY (respuesta fija de seguridad). */
    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "reply", source = "reply")
    @Mapping(target = "source", source = "source")
    @Mapping(target = "choices", source = "choices")
    @Mapping(target = "action", source = "action")
    ChatResponse toResponse(Conversation conversation, String reply, String source, List<String> choices,
                            AgentAction action);
}
