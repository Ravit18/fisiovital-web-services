package com.healthdev.fisiovital.mapper;

import com.healthdev.fisiovital.dto.response.AssistantReplyResponseDTO;
import com.healthdev.fisiovital.model.Conversation;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-22T12:47:07-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Eclipse Adoptium)"
)
@Component
public class AssistantMapperImpl implements AssistantMapper {

    @Override
    public AssistantReplyResponseDTO toResponse(Conversation conversation, String reply, String source) {
        if ( conversation == null && reply == null && source == null ) {
            return null;
        }

        Long conversationId = null;
        if ( conversation != null ) {
            conversationId = conversation.getId();
        }
        String reply1 = null;
        reply1 = reply;
        String source1 = null;
        source1 = source;

        AssistantReplyResponseDTO assistantReplyResponseDTO = new AssistantReplyResponseDTO( conversationId, reply1, source1 );

        return assistantReplyResponseDTO;
    }
}
