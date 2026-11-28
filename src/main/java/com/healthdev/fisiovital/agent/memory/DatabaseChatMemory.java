package com.healthdev.fisiovital.agent.memory;

import com.healthdev.fisiovital.model.ChatMessage;
import com.healthdev.fisiovital.model.Conversation;
import com.healthdev.fisiovital.model.MessageSender;
import com.healthdev.fisiovital.repository.ChatMessageRepository;
import com.healthdev.fisiovital.repository.ConversationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Chat Memory de Spring AI respaldada por las tablas conversations/chat_messages (US17-US20): el historial
 * sobrevive reinicios y se limita a los ultimos {@value #WINDOW} mensajes. El id de conversacion es el de la
 * entidad Conversation.
 */
@Component
public class DatabaseChatMemory implements ChatMemory {

    static final int WINDOW = 20;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    public DatabaseChatMemory(ConversationRepository conversationRepository,
                              ChatMessageRepository chatMessageRepository) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        Conversation conversation = conversationRepository.findById(Long.valueOf(conversationId)).orElseThrow();
        for (Message message : messages) {
            MessageSender sender = switch (message) {
                case UserMessage ignored -> MessageSender.USER;
                case AssistantMessage ignored -> MessageSender.ASSISTANT;
                default -> null;
            };
            if (sender != null && message.getText() != null) {
                String text = sender == MessageSender.ASSISTANT ? storedReply(message.getText()) : message.getText();
                chatMessageRepository.save(conversation.addMessage(sender, text, LocalDateTime.now()));
            }
        }
        conversationRepository.save(conversation);
    }

    /**
     * El modelo responde con salida estructurada ({"reply": "...", "choices": [...]}); en el historial se guarda
     * el texto y, si ofrecio opciones, una linea final "[opciones: a | b]" para que el modelo recuerde que
     * ofrecio y el historial pueda volver a mostrarlas.
     */
    private static String storedReply(String text) {
        try {
            JsonNode root = MAPPER.readTree(text);
            JsonNode reply = root.get("reply");
            if (reply == null || !reply.isTextual()) {
                return text;
            }
            List<String> choices = new ArrayList<>();
            JsonNode options = root.get("choices");
            if (options != null && options.isArray()) {
                options.forEach(o -> {
                    if (o.isTextual() && !o.asText().isBlank()) {
                        choices.add(o.asText().strip().replace("|", "/"));
                    }
                });
            }
            return choices.isEmpty() ? reply.asText() : reply.asText() + ChoicesLine.format(choices);
        } catch (JsonProcessingException e) {
            return text;
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        List<ChatMessage> latest = chatMessageRepository.findByConversationIdOrderByCreatedAtDescIdDesc(
                Long.valueOf(conversationId), PageRequest.of(0, WINDOW));
        List<Message> messages = new ArrayList<>();
        for (int i = latest.size() - 1; i >= 0; i--) {
            ChatMessage m = latest.get(i);
            messages.add(m.getSender() == MessageSender.USER
                    ? new UserMessage(m.getContent()) : new AssistantMessage(m.getContent()));
        }
        return messages;
    }

    @Override
    public void clear(String conversationId) {
        // El historial se conserva como registro de la conversacion; solo vence por inactividad.
    }
}
