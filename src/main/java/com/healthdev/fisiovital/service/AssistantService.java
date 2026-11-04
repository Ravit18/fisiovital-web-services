package com.healthdev.fisiovital.service;

import com.healthdev.fisiovital.dto.request.AskAssistantRequestDTO;
import com.healthdev.fisiovital.dto.response.AssistantReplyResponseDTO;
import com.healthdev.fisiovital.exception.BusinessException;
import com.healthdev.fisiovital.mapper.AssistantMapper;
import com.healthdev.fisiovital.model.ChatMessage;
import com.healthdev.fisiovital.model.Conversation;
import com.healthdev.fisiovital.model.MessageSender;
import com.healthdev.fisiovital.model.Role;
import com.healthdev.fisiovital.repository.ChatMessageRepository;
import com.healthdev.fisiovital.repository.ConversationRepository;
import com.healthdev.fisiovital.repository.UserRepository;
import com.healthdev.fisiovital.security.CurrentUserProvider;
import com.healthdev.fisiovital.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * US17-US20 - Asistente conversacional con Spring AI y Google Gemini. Aplica primero las reglas de
 * seguridad fijas y luego consulta al modelo con el historial de la conversacion; los pacientes ademas
 * pueden reservar y reprogramar mediante las herramientas de AssistantTools.
 */
@Slf4j
@Service
public class AssistantService {

    static final int HISTORY_SIZE = 20;

    private final ChatClient chatClient;
    private final AssistantGuardrails guardrails;
    private final AssistantTools tools;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUser;
    private final AssistantMapper assistantMapper;
    private final MessageSource messageSource;
    private final boolean configured;

    public AssistantService(ChatClient.Builder chatClientBuilder, AssistantGuardrails guardrails,
                            AssistantTools tools, ConversationRepository conversationRepository,
                            ChatMessageRepository chatMessageRepository, UserRepository userRepository,
                            CurrentUserProvider currentUser, AssistantMapper assistantMapper, MessageSource messageSource,
                            @Value("${app.ai.gemini.api-key:}") String apiKey) {
        this.chatClient = chatClientBuilder.build();
        this.guardrails = guardrails;
        this.tools = tools;
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
        this.assistantMapper = assistantMapper;
        this.messageSource = messageSource;
        this.configured = !apiKey.isBlank();
    }

    public AssistantReplyResponseDTO ask(AskAssistantRequestDTO request) {
        UserPrincipal principal = currentUser.get();
        Optional<String> safetyReply = guardrails.check(request.message());
        if (safetyReply.isEmpty() && !configured) {
            throw BusinessException.unavailable("assistant.unavailable");
        }

        Conversation conversation = resolveConversation(request.conversationId(), principal.userId());
        String reply = safetyReply.map(this::msg)
                .orElseGet(() -> callModel(principal.role(), history(conversation), request.message()));

        save(conversation, MessageSender.USER, request.message());
        save(conversation, MessageSender.ASSISTANT, reply);
        return assistantMapper.toResponse(conversation, reply, safetyReply.isPresent() ? "SAFETY" : "AI");
    }

    private Conversation resolveConversation(Long conversationId, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        if (conversationId != null) {
            Conversation existing = conversationRepository.findById(conversationId)
                    .filter(c -> c.belongsTo(userId))
                    .orElseThrow(() -> BusinessException.notFound("assistant.conversation.not.found"));
            if (!existing.isExpired(now)) {
                return existing;
            }
        }
        return conversationRepository.save(new Conversation(userRepository.getReferenceById(userId), now));
    }

    /** Ultimos mensajes de la conversacion en orden cronologico, para que el modelo tenga el contexto. */
    private List<Message> history(Conversation conversation) {
        List<ChatMessage> latest = chatMessageRepository.findByConversationIdOrderByCreatedAtDescIdDesc(
                conversation.getId(), PageRequest.of(0, HISTORY_SIZE));
        List<Message> messages = new ArrayList<>();
        for (int i = latest.size() - 1; i >= 0; i--) {
            ChatMessage m = latest.get(i);
            messages.add(m.getSender() == MessageSender.USER
                    ? new UserMessage(m.getContent()) : new AssistantMessage(m.getContent()));
        }
        return messages;
    }

    private String callModel(Role role, List<Message> history, String message) {
        try {
            ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                    .system(AssistantPrompts.system(role, LocalDateTime.now(), LocaleContextHolder.getLocale()))
                    .messages(history)
                    .user(message);
            if (role == Role.PATIENT) {
                spec = spec.tools(tools);
            }
            String content = spec.call().content();
            if (content == null || content.isBlank()) {
                throw BusinessException.unavailable("assistant.unavailable");
            }
            return content.trim();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("El asistente de IA no respondio: {}", e.getMessage());
            throw BusinessException.unavailable("assistant.unavailable");
        }
    }

    private void save(Conversation conversation, MessageSender sender, String content) {
        ChatMessage message = conversation.addMessage(sender, content, LocalDateTime.now());
        chatMessageRepository.save(message);
        conversationRepository.save(conversation);
    }

    private String msg(String key) {
        return messageSource.getMessage(key, null, key, LocaleContextHolder.getLocale());
    }
}
